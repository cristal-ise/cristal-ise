# In-Memory Lookup Module Guidelines

## Purpose
Implementation of `ClusterStorage` and `Lookup` interfaces in-memory. Primarily used for functional testing and bootstrapping.

## Key Features
- Stores Items in memory for quick access.
- Non-persistent; data is lost on restart.
- Limited search functionality.

## Testing
- Used in unit and functional tests where full persistence is not required.

## Guidelines for Junie
- Use this module when quick, lightweight storage is needed for tests.
- Note that `searchAliases` and certain `search` methods are currently unimplemented.
