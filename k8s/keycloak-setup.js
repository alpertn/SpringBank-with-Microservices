const http = require('http');

const KEYCLOAK_URL = 'http://localhost:8080';
const ADMIN_USER = 'alper123';
const ADMIN_PASS = 'alper123A';
const REALM_NAME = 'banking';
const CLIENT_ID = 'banking-app';
const CLIENT_SECRET = 'Iopu5gL8VfLtIX39701gkwd6iCd7gKW6';

async function request(path, options, body) {
    return new Promise((resolve, reject) => {
        const req = http.request(`${KEYCLOAK_URL}${path}`, options, (res) => {
            let data = '';
            res.on('data', (chunk) => data += chunk);
            res.on('end', () => {
                if (res.statusCode >= 200 && res.statusCode < 300) {
                    try {
                        resolve(data ? JSON.parse(data) : null);
                    } catch (e) {
                        resolve(data);
                    }
                } else {
                    reject(new Error(`HTTP ${res.statusCode}: ${data}`));
                }
            });
        });
        req.on('error', reject);
        if (body) {
            req.write(typeof body === 'string' ? body : JSON.stringify(body));
        }
        req.end();
    });
}

async function getAdminToken() {
    console.log('[1/8] Getting Admin Token...');
    const body = new URLSearchParams({
        client_id: 'admin-cli',
        username: ADMIN_USER,
        password: ADMIN_PASS,
        grant_type: 'password'
    }).toString();

    const result = await request('/realms/master/protocol/openid-connect/token', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'Content-Length': Buffer.byteLength(body)
        }
    }, body);
    console.log('      Token acquired.');
    return result.access_token;
}

async function createRealm(token) {
    console.log(`[2/8] Creating realm '${REALM_NAME}'...`);
    try {
        await request('/admin/realms', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        }, { realm: REALM_NAME, enabled: true });
        console.log('      Realm created.');
    } catch (e) {
        if (e.message.includes('409')) console.log('      Realm already exists. Moving on.');
        else throw e;
    }
}

async function createRole(token, roleName) {
    console.log(`[3/8] Creating role '${roleName}'...`);
    try {
        await request(`/admin/realms/${REALM_NAME}/roles`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        }, { name: roleName });
        console.log('      Role created.');
    } catch (e) {
        if (e.message.includes('409')) console.log('      Role already exists. Moving on.');
        else throw e;
    }
}

async function getRoleConfig(token, roleName) {
    return request(`/admin/realms/${REALM_NAME}/roles/${roleName}`, {
        method: 'GET',
        headers: { 'Authorization': `Bearer ${token}` }
    });
}

async function createClient(token) {
    console.log(`[4/8] Creating client '${CLIENT_ID}'...`);
    try {
        await request(`/admin/realms/${REALM_NAME}/clients`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        }, {
            clientId: CLIENT_ID,
            secret: CLIENT_SECRET,
            enabled: true,
            publicClient: false,
            directAccessGrantsEnabled: true,
            serviceAccountsEnabled: true,
            standardFlowEnabled: true
        });
        console.log('      Client created.');
    } catch (e) {
        if (e.message.includes('409')) console.log('      Client already exists. Moving on.');
        else throw e;
    }
}

async function createUser(token) {
    console.log(`[5/8] Creating test user...`);
    try {
        await request(`/admin/realms/${REALM_NAME}/users`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        }, {
            username: 'testuser@bank.com',
            email: 'testuser@bank.com',
            firstName: 'Test',
            lastName: 'User',
            enabled: true,
            emailVerified: true,
            credentials: [{ type: 'password', value: 'pass123', temporary: false }]
        });
        console.log('      User created.');
    } catch (e) {
        if (e.message.includes('409')) console.log('      User already exists. Moving on.');
        else throw e;
    }
}

async function getUser(token, username) {
    const users = await request(`/admin/realms/${REALM_NAME}/users?username=${username}`, {
        method: 'GET',
        headers: { 'Authorization': `Bearer ${token}` }
    });
    return users[0];
}

async function assignRole(token, userId, roleConfig) {
    console.log(`[6/8] Assigning role '${roleConfig.name}' to user...`);
    try {
        await request(`/admin/realms/${REALM_NAME}/users/${userId}/role-mappings/realm`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        }, [roleConfig]);
        console.log('      Role assigned.');
    } catch (e) {
        console.log(`      Could not assign role: ${e.message}`);
    }
}

async function run() {
    try {
        console.log('Starting automated Keycloak setup via Node.js...');
        const token = await getAdminToken();
        await createRealm(token);
        await createRole(token, 'USER');
        await createRole(token, 'ADMIN');
        await createClient(token);
        await createUser(token);
        
        console.log('[7/8] Fetching full role/user references for mapping...');
        const user = await getUser(token, 'testuser@bank.com');
        const role = await getRoleConfig(token, 'USER');
        
        if (user && role) {
            await assignRole(token, user.id, role);
        } else {
             console.log('      Could not fetch User/Role for assignment.');
        }

        console.log('[8/8] Done!');
        console.log('============================================');
        console.log('  Keycloak Successfully Configured!');
        console.log('============================================');
    } catch (e) {
        console.error('An error occurred during Keycloak setup:', e);
        process.exit(1);
    }
}

run();
