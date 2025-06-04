# KenyaEMR Cashier Module
[![Build Status](https://github.com/palladiumkenya/openmrs-module-kenyaEMRCashier/actions/workflows/build-deploy.yml/badge.svg)](https://github.com/palladiumkenya/openmrs-module-kenyaEMRCashier/actions/workflows/build-deploy.yml)

The KenyaEMR Cashier Module is an OpenMRS module that provides comprehensive cashier functionality for healthcare facilities. It enables efficient management of financial transactions, inventory, and receipt generation within the KenyaEMR system.

## Major Functionalities

### 1. Cashier Operations
- Patient billing and payment processing
- Multiple payment method support
- Receipt generation and printing
- Transaction history and reporting
- Daily cash reconciliation

### 2. Item Management
- Stock item tracking
- Price list management
- Item categorization
- Stock level monitoring
- Automatic stock updates

### 3. Financial Management
- Revenue tracking
- Payment reconciliation
- Financial reporting
- Transaction auditing
- Daily/monthly financial summaries

### 4. Integration Features
- Seamless integration with KenyaEMR core
- Stock management module integration
- Order expansion module integration
- REST API support for external systems

## Installation

1. Download the latest release from the [releases page](https://github.com/palladiumkenya/openmrs-module-kenyaEMRCashier/releases)
2. Install the module through the OpenMRS admin interface
3. Configure the module settings as per your facility's requirements

## Development Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/palladiumkenya/openmrs-module-kenyaEMRCashier.git
   ```

2. Build the module:
   ```bash
   mvn clean install
   ```

3. Deploy to your OpenMRS instance:
   ```bash
   # Copy the built .omod file to your OpenMRS modules directory
   cp omod/target/kenyaemr.cashier-*.omod /path/to/openmrs/modules/
   ```
   
   Note: Replace `/path/to/openmrs/modules/` with your actual OpenMRS modules directory path. 
   The default location is typically:
   - Linux: `/var/lib/OpenMRS/modules/`
   - Windows: `C:\Program Files\OpenMRS\modules\`
   - Mac: `/Applications/OpenMRS/modules/`

## Release Process

### Prerequisites
- Write access to the repository
- Maven installed and configured
- Git configured with appropriate credentials

### Steps to Create a Release

1. **Prepare for Release**
   - Ensure all tests pass
   - Update documentation
   - Verify all changes are committed
   - Check for any SNAPSHOT dependencies

2. **Create Release Tag**
   ```bash
   # Create and push a new tag
   git tag v4.2.2
   git push origin v4.2.2
   ```

3. **Automated Release Process**
   The CI/CD pipeline will automatically:
   - Build the release version
   - Publish to Nexus repository
   - Increment version for next development cycle
   - Update the master branch

4. **Verify Release**
   - Check the [Nexus repository](https://nexus.mekomsolutions.net) for the published artifact
   - Verify the new SNAPSHOT version in master branch
   - Test the released version in a staging environment

### Version Management
- Follows semantic versioning (MAJOR.MINOR.PATCH)
- SNAPSHOT versions are used during development
- Release versions are created from tags
- Version increment is automated after release

## Support and Documentation

- [User Documentation](https://kenyahmis.org/documentation)
- [Issue Tracker](https://thepalladiumgroup.atlassian.net/)
- [Demo Server](https://dev.kenyahmis.org)

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

This project is licensed under the [OpenMRS Public License](LICENSE.txt).
