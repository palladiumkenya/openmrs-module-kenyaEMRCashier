# Visit-Bill Association Feature

## Overview

The KenyaEMR Cashier module now supports associating bills with visits, enabling better tracking of care provided during specific patient visits and supporting in-patient billing scenarios.

## Use Cases

### 1. In-Patient Billing
- **Length of Stay Calculation**: Associate bills with visits to calculate the number of days a patient has been admitted for billing purposes
- **Daily Rate Billing**: Apply daily rates based on visit duration
- **Care Episode Tracking**: Link all services provided during a specific admission to the visit

### 2. Care Linking
- **Service Attribution**: Link specific care services (medications, procedures, tests) to the visit during which they were provided
- **Quality Assurance**: Track care quality by analyzing services provided during specific visits
- **Audit Trail**: Maintain clear audit trails of what services were provided when

## Implementation Details

### Database Changes
- Added `visit_id` column to `cashier_bill` table
- Foreign key constraint to `visit(visit_id)` table
- Index on `visit_id` for performance

### Model Changes
- Added `visit` field to `Bill` model
- Added getter/setter methods for visit association

### Service Layer
- `IBillService.getBillsByVisit(Visit visit)` - Get all bills for a specific visit
- `IBillService.getBillsByVisitId(Integer visitId)` - Get all bills for a visit ID
- `IBillService.associateBillWithVisit(Bill bill, Visit visit)` - Associate a bill with a visit
- `IBillService.disassociateBillFromVisit(Bill bill)` - Remove visit association

### REST API
- Added `visit` property to bill representations
- Added `visitUuid` parameter to bill search endpoint
- Support for creating/updating bills with visit association

### Automatic Association
- Bills are automatically associated with visits when orders are created through the `GenerateBillFromOrderable` advice
- The visit is extracted from the order's encounter and associated with the bill

## API Usage Examples

### Search Bills by Visit
```http
GET /openmrs/ws/rest/v1/kenyaemr/cashier/bill?visitUuid=123e4567-e89b-12d3-a456-426614174000
```

### Create Bill with Visit Association
```json
{
  "patient": "123e4567-e89b-12d3-a456-426614174000",
  "visit": "123e4567-e89b-12d3-a456-426614174000",
  "lineItems": [...],
  "status": "PENDING"
}
```

### Associate Existing Bill with Visit
```http
POST /openmrs/ws/rest/v1/kenyaemr/cashier/bill/{billUuid}
{
  "visit": "123e4567-e89b-12d3-a456-426614174000"
}
```

## Backward Compatibility

- The visit association is **optional** - existing bills without visit associations continue to work normally
- All existing functionality remains unchanged
- The feature is additive and doesn't break existing workflows

## Migration

The database migration is handled automatically by Liquibase:
- Adds `visit_id` column to existing `cashier_bill` table
- Creates foreign key constraint and index
- No data migration required for existing bills

## Testing

The feature includes comprehensive tests:
- `getBillsByVisit_shouldReturnBillsForVisit()`
- `getBillsByVisitId_shouldReturnBillsForVisitId()`
- `associateBillWithVisit_shouldAssociateBillWithVisit()`
- `disassociateBillFromVisit_shouldRemoveVisitAssociation()`
- `searchBillsByVisit_shouldReturnBillsForVisit()`

## Future Enhancements

1. **Visit Duration Billing**: Calculate billing based on visit start/stop times
2. **Visit Type Specific Pricing**: Different pricing for different visit types (inpatient, outpatient, emergency)
3. **Visit-based Reporting**: Generate reports showing care costs per visit
4. **Integration with Other Modules**: Enhanced integration with clinical modules for comprehensive care tracking 