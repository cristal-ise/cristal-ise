# Devbox Module Guidelines

## Purpose
Vagrant-based environment setup for CRISTAL-iSE developers. Provides a pre-configured VM with all required dependencies.

## Key Features
- Pre-installed PostgreSQL, Java, and Maven.
- Automated system setup via `setup.sh`.
- Persistent storage for database data.

## Technologies
- **Vagrant**: VM orchestration.
- **VirtualBox**: Hypervisor.
- **Ubuntu**: OS in the VM.

## Usage
- `vagrant up` to start the VM.
- `vagrant ssh` to access the environment.
- Follow `README.md` for manual installation instructions.

## Guidelines for Junie
- Use this environment if local installation is not feasible.
- Ensure any changes to `setup.sh` are compatible with Ubuntu LTS.
- Document any new system dependencies in `README.md`.
