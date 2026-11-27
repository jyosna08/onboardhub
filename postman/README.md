# OnboardHub Postman Collection

## Import

In Postman, choose **Import**, select `OnboardHub.postman_collection.json` and `OnboardHub.local.postman_environment.json`, then select the **OnboardHub Local** environment.

## Environment variables

- `baseUrl`: gateway URL; defaults to `http://localhost:8090`.
- `adminEmail` and `adminPassword`: set these to the same values as `BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` in the running Compose stack. They are empty in the exported environment.
- `testPassword`: `Password123`, used only for disposable accounts created by the collection.
- Runtime values are empty in the exported environment and populated while requests run: `suffix`, `adminToken`, `hrToken`, `itToken`, `empUserToken`, `hrEmail`, `itEmail`, `empUserEmail`, `employeeEmail`, `employeeId`, `equipmentTaskId`, `hrTaskId`, `accountTaskId`, `laptopAssetId`, `laptopAssetTag`, `monitorAssetId`, `serialA`, and `serialB`.

The collection creates uniquely suffixed test accounts, an employee and assets. Its collection pre-request script sets `suffix` once when empty. For repeat Newman runs, pass the exported environment again; in Postman, clear its populated `suffix` before rerunning.

## Run order

Run folders in order from `00 Setup` through `06 Health`. Setup logs in the bootstrap admin, creates HR and IT users, registers an employee account, then logs in each test role.

## Newman

From the repository root, run:

```sh
npx newman run postman/OnboardHub.postman_collection.json -e postman/OnboardHub.local.postman_environment.json --env-var "adminEmail=..." --env-var "adminPassword=..."
```

