
kubectl logs -n banking-microservices -l "app in (gateway, auth-service, user-service, transaction-service, money-service, fraud-service)" -f --max-log-requests=50
goto AFTER
