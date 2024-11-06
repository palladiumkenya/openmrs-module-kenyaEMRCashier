# KenyaEMR Billing Exemptions Guide

## Overview
This guide explains how to configure billing exemptions in KenyaEMR for different services and commodities. Exemptions can be set up for specific groups of patients, programs, or services that should not be billed.

## Understanding Exemptions
Billing exemptions can be applied to two main categories:
1. **Services** - Medical procedures, consultations, and other healthcare services
2. **Commodities** - Medical supplies, medications, and other physical items

## Exemption Categories
You can set up exemptions based on different criteria:

- `all` - Applies to all patients
- `program:HIV` - Applies to patients in the HIV program
- `program:TB` - Applies to patients in the TB program
- `age<5` - Applies to children under 5 years
- `visitAttribute:prisoner` - Applies to patients marked as prisoners

## How to Configure Exemptions

### Basic Structure
Exemptions are configured using a simple format with the following information:
- Category name (e.g., "all", "program:HIV")
- List of items under each category
- Each item needs a concept ID and description

### Example Configuration
Here's how to structure your exemptions:

```json
{
  "services": {
    "all": [
      {"concept": 111, "description": "Malaria"},
      {"concept": 112, "description": "Typhoid"}
    ],
    "program:HIV": [
      {"concept": 113, "description": "X-ray"}
    ]
  }
}
```

### Step-by-Step Guide

1. **Identify Exempt Items**
   - Make a list of services/commodities to be exempted
   - Note down their concept IDs from the system
   - Write clear descriptions for each item

2. **Choose Exemption Categories**
   - Decide which groups should receive exemptions
   - Use appropriate category names (e.g., "all", "program:HIV")

3. **Group Items by Category**
   - List items under appropriate categories
   - You can include the same item in multiple categories

## Tips for Managing Exemptions

1. **Organization**
   - Keep related items together under the same category
   - Use clear, descriptive names for items
   - Maintain a separate list of concept IDs and descriptions

2. **Documentation**
   - Keep a record of why each exemption was created
   - Document any special conditions or requirements
   - Update documentation when changes are made

3. **Regular Review**
   - Review exemptions periodically
   - Remove outdated exemptions
   - Add new exemptions as policies change

## Common Scenarios

### Example 1: Adding HIV Program Exemptions
To exempt all HIV-related services:
```json
{
  "services": {
    "program:HIV": [
      {"concept": 111, "description": "CD4 Count"},
      {"concept": 112, "description": "Viral Load"},
      {"concept": 113, "description": "ARV Consultation"}
    ]
  }
}
```

### Example 2: Child Healthcare Exemptions
To exempt services for children under 5:
```json
{
  "services": {
    "age<5": [
      {"concept": 111, "description": "Vaccination"},
      {"concept": 112, "description": "Growth Monitoring"},
      {"concept": 113, "description": "Pediatric Consultation"}
    ]
  }
}
```

## Troubleshooting

### Common Issues

1. **Exemptions Not Applied**
   - Verify the concept ID is correct
   - Check the category name spelling
   - Ensure the JSON format is valid

2. **Multiple Exemptions Conflict**
   - Review overlapping categories
   - Verify priority of exemptions
   - Check for duplicate entries

### Getting Help
If you encounter issues:
1. Review this documentation
2. Check your configuration format
3. Contact your system administrator
4. Consult Palladium Support via the support email provided in the project

## Best Practices

1. **Regular Maintenance**
   - Review exemptions quarterly
   - Update descriptions as needed
   - Remove unused exemptions
   - Document all changes

2. **Naming Conventions**
   - Use clear category names
   - Include detailed descriptions
   - Maintain consistency in naming

3. **Testing**
   - Test new exemptions before implementing
   - Verify exemptions work as expected
   - Check billing reports for accuracy

Remember: Changes to exemptions can significantly impact billing and patient care. Always double-check your configurations and consult with relevant stakeholders before making changes.

## Setting Up Exemptions

### 1. Access Global Properties
1. Log in as administrator
2. Go to Administration → Manage Global Properties
3. Find or create `kenyaemr.billing.exemptions`

### 2. Configure Exemptions
Copy and paste your exemption rules in this format:
```json
{
  "services": {
    "all": [
      {"concept": 111, "description": "Malaria"},
      {"concept": 112, "description": "Typhoid"}
    ],
    "program:HIV": [
      {"concept": 113, "description": "X-ray"}
    ]
  },
  "commodities": {
    "all": [
      {"concept": 114, "description": "ACT"}
    ]
  }
}
```

### 3. Available Categories
- `all` - For everyone
- `program:HIV` - HIV program patients
- `program:TB` - TB program patients
- `age<5` - Children under 5
- `visitAttribute:prisoner` - Prisoners

### 4. Save & Test
1. Click Save
2. Test with a sample patient
3. Verify exemptions are working

## Troubleshooting
- If changes don't work, check JSON format
- Ensure concept IDs are correct
- Clear browser cache and refresh

Need help? Contact Palladium Support via the support email provided in the project
