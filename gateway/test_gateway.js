const http = require('http');

const BASE_URL = 'http://localhost:8095';

async function fetchAPI(path, method = 'GET', body = null, token = null) {
    const headers = {
        'Content-Type': 'application/json'
    };
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }

    const options = {
        method: method,
        headers: headers
    };

    if (body) {
        options.body = JSON.stringify(body);
    }

    try {
        const res = await fetch(BASE_URL + path, options);
        const text = await res.text();
        let data = null;
        try {
            data = JSON.parse(text);
        } catch {
            data = text;
        }
        return { status: res.status, ok: res.ok, data };
    } catch (e) {
        return { status: 0, ok: false, error: e.message };
    }
}

async function runTests() {
    console.log("=== BROWSERLESS FRONTEND GATEWAY TESTS ===");
    
    const ts = Date.now();
    const testUser = {
        email: `testuser_${ts}@example.com`,
        Name: "Test",
        surname: "User",
        password: "password123"
    };

    console.log("\n1. Test Registration:", testUser.email);
    const regRes = await fetchAPI('/api/auth-service/v1/auth/register', 'POST', testUser);
    console.log("Status:", regRes.status);
    if (!regRes.ok) {
        console.error("FAIL: Registration failed.", regRes.data);
        return;
    }
    console.log("PASS: Registered successfully.");


    console.log("\n2. Test Login");
    const loginRes = await fetchAPI('/api/auth-service/v1/auth/login', 'POST', { email: testUser.email, password: testUser.password });
    console.log("Status:", loginRes.status);
    if (!loginRes.ok) {
        console.error("FAIL: Login failed.", loginRes.data);
        return;
    }
    const token = loginRes.data.access_token;
    if (!token) {
        console.error("FAIL: No access token received.", loginRes.data);
        return;
    }
    console.log("PASS: Logged in successfully. Token acquired.");


    console.log("\n3. Get Balance Information");
    let balanceRes = await fetchAPI('/api/money-service/v1/accounts/balance-info', 'GET', null, token);
    console.log("Status:", balanceRes.status);
    
    // Sometimes balance info might need initial creation or wait for Kafka topic
    let retries = 5;
    while (!balanceRes.ok && retries > 0) {
        console.log("Waiting for account creation via Kafka... retrying");
        await new Promise(r => setTimeout(r, 2000));
        balanceRes = await fetchAPI('/api/money-service/v1/accounts/balance-info', 'GET', null, token);
        retries--;
    }

    if (!balanceRes.ok) {
        console.error("FAIL: Could not get balance information.", balanceRes.data);
        return;
    }
    
    const accountIban = balanceRes.data.userIban;
    console.log("PASS: Balance:", balanceRes.data.money, "IBAN:", accountIban);
    if (!accountIban) {
        console.error("FAIL: IBAN is missing.");
        return;
    }


    console.log("\n4. Test Deposit");
    const depositPayload = {
        accountIban: accountIban,
        amount: 500,
        description: "Test Deposit"
    };
    const depRes = await fetchAPI('/api/transaction-service/v1/transactions/deposit', 'POST', depositPayload, token);
    console.log("Status:", depRes.status);
    if (!depRes.ok) {
        console.error("FAIL: Deposit transaction failed.", depRes.data);
        // We do not stop here, maybe it is a business error we can investigate
    } else {
        console.log("PASS: Deposit accepted.");
    }


    console.log("\n5. Wait 2 seconds for Kafka processing, then Check Balance Again");
    await new Promise(r => setTimeout(r, 2000));
    let balanceCheckRes = await fetchAPI('/api/money-service/v1/accounts/balance-info', 'GET', null, token);
    console.log("Status:", balanceCheckRes.status);
    if (balanceCheckRes.ok) {
        console.log("PASS: New Balance is", balanceCheckRes.data.money);
    } else {
        console.error("FAIL: Could not get balance after deposit.", balanceCheckRes.data);
    }


    console.log("\n6. Test Withdraw");
    const withdrawPayload = {
        accountIban: accountIban,
        amount: 200,
        description: "Test Withdraw"
    };
    const witRes = await fetchAPI('/api/transaction-service/v1/transactions/withdraw', 'POST', withdrawPayload, token);
    console.log("Status:", witRes.status);
    if (!witRes.ok) {
        console.error("FAIL: Withdraw transaction failed.", witRes.data);
    } else {
        console.log("PASS: Withdraw accepted.");
    }


    console.log("\n7. Wait 2 seconds for Kafka processing, then Check final Balance");
    await new Promise(r => setTimeout(r, 2000));
    let finalBalanceRes = await fetchAPI('/api/money-service/v1/accounts/balance-info', 'GET', null, token);
    if (finalBalanceRes.ok) {
        console.log("PASS: Final Balance is", finalBalanceRes.data.money);
    } else {
        console.error("FAIL: Could not get final balance.", finalBalanceRes.data);
    }
    
    console.log("\n=== TESTS FINISHED ===");
}

runTests();
