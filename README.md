#OverPermissions

A feature packed, simple to use SQL based SuperPermissions system for Bukkit.

##Commands
OverPermissions comes standard with commands for editing group and user data easily.

All commands support tab completion on the majority of their elements.

### Syntax markers
 * `( )` marks optional arguments, such as `(world)`
 * `[ ]` marks required arguments, such as `[group]`
 * `' '` marks string literals, such as `'clear'`
 * `...` marks a variable amount of arguments, such as `values...`
 * `|` marks multiple options, such as `[value | 'literal']`
 
### Defining time

Time can be defined by a repeating series of [amount] [unit].

For example: `5d12h15s` would be 5 days, 12 hours, and 15 seconds.

You can also use decimals, for example: `.5y1.2h` would be half a year, and 1.2 hours.

Months are standardized to 30 days.

###Group Command definitions
####/groupadd
Adds a single permission node to a single group.
 
 * Syntax: `/groupadd [group] [permission] (world)`
 * Permission: `overpermissions.groupadd`
 * Aliases: `/ga`

####/groupremove
Removes a single permission node from a single group.

 * Syntax: `/groupremove [group] [permission] (world)`
 * Permission: `overpermissions.groupremove`
 * Aliases: `/gr`
 
####/groupaddtemp
Adds a temporary permission to a single group. This permission is stored apart from 'permanent' permissions, and can co-exist with them.

 * Syntax: `/groupaddtemp [group] [permission] [time] (world)`
 * Permission: `overpermissions.groupaddtemp`
 * Aliases: `/gat`

####/groupremovetemp
Removes a temporary permission from a single group. Temporary permissions are stored separately from 'permanent' permissions, and can co-exist with them.

 * Syntax: `/groupremovetemp [group] [permission] (world)`
 * Permission: `overpermissions.groupremovetemp`
 * Aliases: `/grt`

####/groupcreate
Creates a group with the specified name and priority. Names are unique and case insensitive.

Groups with higher priorities will override the metadata values of groups with lower priorities.

 * Syntax: `/groupcreate [name] [priority]`
 * Permission: `overpermissions.groupcreate`
 * Aliases: `/gc`, `/creategroup`
 
####/groupdelete
Deletes a single group. You can't delete the default group.

Groups that have this group as a parent or child will be updated accordingly.

 * Syntax: `/groupdelete [name]`
 * Permission: `overpermissions.groupdelete`
 * Aliases: `/gc`, `/creategroup`
 
####/groupaddparent
Adds a parent to a group.

Parents will add all of their nodes and metadata to the combined pool.

 * Syntax: `/groupaddparent [group] [parent]`
 * Permission: `overpermissions.groupaddparent`
 * Aliases: `/gap`
 
####/groupremoveparent
Removes a parent from a group.

 * Syntax: `/groupremoveparent [group] [parent]
 * Permission: `overpermissions.groupremoveparent`
 * Aliases: `/grp`
 
####/groupsetmeta
Sets the metadata entry at a specified key to a specified value.

If the value is 'clear' the value will be unset.

 * Syntax: `/groupsetmeta [group] [key] [value | 'clear'] (world)`
 * Permission: `overpermissions.groupsetmeta`
 * Aliases: `gsm`

###Player command definitions
####/playeradd
Adds a specific permission node to a single player.

 * Syntax: `/playeradd [player] [node] (world)`
 * Permission: `overpermissions.playeradd`
 * Aliases: `pa`

####/playerremove
Removes a specific node from a single player.

 * Syntax: `/playerremove [player] [node] (world)`
 * Permission: `overpermissions.playerremove`
 * Aliases: `pr`
 
####/playeraddtemp
Adds a specific temporary permission to a single player for the specified amount of time.

 * Syntax: `/playeraddtemp [player] [node] [time] (world)`
 * Permission: `overpermissions.playeraddtemp`
 * Aliases: `pat`
 
####/playerremovetemp
Removes a the specified temporary permission from a single player.

 * Syntax: `/playerremovetemp [player] [node] (world)`
 * Permission: `overpermissions.playerremovetemp`
 * Aliases: `prt`
 
####/playeraddgroup
Adds a group to the specified player's group list

 * Syntax: `/playeraddgroup [player] [group]`
 * Permission: `overpermissions.playeraddgroup`
 * Aliases: `pag`
 
####/playerremovegroup
Removes a single group from the player's group list.

 * Syntax: `/playerremovegroup [player] [group]`
 * Permission: `overpermissions.playerremovegroup`
 * Aliases: `prg`
 
####/playersetgroup
Removes all of a player's groups, then sets the player's group to the specified group.
 
  * Syntax: `/playersetgroup [player] [group]`
  * Permission: `overpermissions.playersetgroup`
  * Aliases: `groupset`, `playerset`, `psg`

####/playercheck
Find information on a specific node for a specific player.

This command is good if you want to check if a specified user has a specific permission.

 * Syntax: `/playercheck [player] [node] (world)`
 * Permission: `overpermissions.playercheck`
 * Aliases: `pc`