Loop, OrSplit and XOSplit use Routing Expression or Script to calculate which branch/edge is enabled. This in turn activates all the activities along of those edges.

- Script/Expression shall return a comma-separated list of string to identify edges
- Each outgoning edge of the Split shall define an Alias property. Check javadoc [Split.isInTable()](https://javadoc.io/doc/org.cristalise/cristalise-kernel/latest/org/cristalise/kernel/lifecycle/instance/Split.html) for more information

The following values are accepted in the Alias property of the edge:
- `miss | misses` - match with one of the values
- `!misses` - match if not equals with misses
- `*miss*` - match if contains miss 
- `*miss` - match if starts with miss
- `miss*` - match if ends with miss
