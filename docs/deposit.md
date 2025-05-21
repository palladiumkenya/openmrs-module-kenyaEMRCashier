# KenyaEMR Cashier Module - Deposit API Documentation

## Base URL

```
/rest/v1/cashier/deposit
```

## Authentication

All endpoints require authentication. Include the session ID in the request header:

```
Cookie: JSESSIONID=<session_id>
```

## 1. Create a Deposit

### Endpoint

```
POST /rest/v1/cashier/deposit
```

### Request Body

```json
{
  "patient": "patient-uuid",
  "amount": 1000.0,
  "depositType": "Surgery",
  "status": "PENDING",
  "referenceNumber": "DEP001",
  "description": "Deposit for surgery"
}
```

### Response (201 Created)

```json
{
  "uuid": "deposit-uuid-1",
  "display": "DEP001 - Surgery",
  "patient": {
    "uuid": "patient-uuid",
    "display": "Patient Name"
  },
  "amount": 1000.0,
  "depositType": "Surgery",
  "status": "PENDING",
  "referenceNumber": "DEP001",
  "description": "Deposit for surgery",
  "transactions": [],
  "dateCreated": "2024-03-26T00:00:00.000+0000",
  "voided": false,
  "availableBalance": 1000.0
}
```

## 2. Apply Deposit to a Bill Line Item

### Endpoint

```
POST /rest/v1/cashier/deposit/{deposit-uuid}/transaction
```

### Request Body

```json
{
  "billLineItem": "bill-line-item-uuid",
  "amount": 500.0,
  "transactionType": "APPLY",
  "reason": "Applied to surgery bill"
}
```

### Response (201 Created)

```json
{
  "uuid": "transaction-uuid-1",
  "billLineItem": {
    "uuid": "bill-line-item-uuid",
    "display": "Surgery - $500.00"
  },
  "amount": 500.0,
  "transactionType": "APPLY",
  "reason": "Applied to surgery bill",
  "dateCreated": "2024-03-26T00:00:00.000+0000",
  "voided": false
}
```

## 3. Reverse a Deposit Transaction

### Endpoint

```
DELETE /rest/v1/cashier/deposit/{deposit-uuid}/transaction/{transaction-uuid}
```

### Request Body

```json
{
  "reason": "Wrong application"
}
```

### Response (204 No Content)

## 4. Get Deposit Details

### Endpoint

```
GET /rest/v1/cashier/deposit/{deposit-uuid}
```

### Response (200 OK)

```json
{
  "uuid": "deposit-uuid-1",
  "display": "DEP001 - Surgery",
  "patient": {
    "uuid": "patient-uuid",
    "display": "Patient Name"
  },
  "amount": 1000.0,
  "depositType": "Surgery",
  "status": "ACTIVE",
  "referenceNumber": "DEP001",
  "description": "Deposit for surgery",
  "transactions": [
    {
      "uuid": "transaction-uuid-1",
      "billLineItem": {
        "uuid": "bill-line-item-uuid",
        "display": "Surgery - $500.00"
      },
      "amount": 500.0,
      "transactionType": "APPLY",
      "reason": "Applied to surgery bill",
      "dateCreated": "2024-03-26T00:00:00.000+0000",
      "voided": false
    }
  ],
  "dateCreated": "2024-03-26T00:00:00.000+0000",
  "voided": false,
  "availableBalance": 500.0
}
```

## 5. Search Deposits

### Endpoint

```
GET /rest/v1/cashier/deposit
```

### Query Parameters

- `patient`: Patient UUID to find deposits for a specific patient
- `referenceNumber`: Reference number to find a specific deposit
- `includeAll`: Boolean to include voided deposits (default: false)

### Example

```
GET /rest/v1/cashier/deposit?patient=patient-uuid&includeAll=false
```

### Response (200 OK)

```json
{
    "results": [
        {
            "uuid": "deposit-uuid-1",
            "display": "DEP001 - Surgery",
            "patient": {
                "uuid": "patient-uuid",
                "display": "Patient Name"
            },
            "amount": 1000.00,
            "depositType": "Surgery",
            "status": "ACTIVE",
            "referenceNumber": "DEP001",
            "description": "Deposit for surgery",
            "transactions": [...],
            "dateCreated": "2024-03-26T00:00:00.000+0000",
            "voided": false,
            "availableBalance": 500.00
        }
    ]
}
```

## Deposit Status Values

- `PENDING`: Deposit has been created but not yet processed
- `ACTIVE`: Deposit has been processed and is active
- `USED`: Deposit has been fully used
- `REFUNDED`: Deposit has been refunded
- `VOIDED`: Deposit has been voided

## Transaction Types

- `APPLY`: Apply deposit to a bill line item
- `REFUND`: Refund unused deposit amount
- `REVERSE`: Reverse an applied deposit

## Error Responses

### 400 Bad Request

```json
{
  "error": {
    "message": "The deposit to save must be defined.",
    "code": "invalid.request"
  }
}
```

### 404 Not Found

```json
{
  "error": {
    "message": "Deposit not found",
    "code": "not.found"
  }
}
```

### 403 Forbidden

```json
{
  "error": {
    "message": "User does not have sufficient privileges",
    "code": "insufficient.privileges"
  }
}
```
