# ItemTable library documentation

ItemTable shows a list of Items in a table format. It is a single component based on PrimeNG p-table features. It can be used in these scenarios:

- List of Items of the same Type, e.g. WorkOrders, normally linked with a MenuItem 
- List of Items in a Collection of an Item, e.g. Addresses of a Customer

## Component
ItemTable is linked with a Schema that definies its coloumns, it is either the MasterSchema or the Schema describing the Collection members.

### Inputs: 
- UUID of the Item 'requesting' to show the table (e.g. DomainContext, Factory Item, Item with the Collections)
- Name of the Collection (can be null)
- UUID of the Schema Item describing the table

### Outputs:
- UUID of the selected Item in the table 


## Service

### getTableConfig()
- read the 
- includes mapping of columns with record names in result

### getTable()
- reads the database for the list of selected Items
- supports paging, ordering and filtering
