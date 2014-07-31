#Wildcards for OverPermissions
##Support
Wildcard support isn't native to OverPermissions, API methods won't use wildcard nodes.

##Types
The way each permissible type works is as follows:

###No wildcards
Just check if the permission node is in the store of permission nodes, literally.

###Half Wildcards
Half wildcards are bounded by periods (.)

The string is split by periods, and then put into a tree. Wildcard nodes will access all children, whereas 'regular' ones will match a specific string.

##Node Tree Datastructure
The Node Tree is a *fairly* simple implementation of a tree.

Each node of the tree has two sublists, the 'containers' , and the 'physical nodes.'

Physical nodes are the ones at the end of permissions, such as 'this.is.my.*physicalnode*'

Container nodes are any nodes that aren't at the end of permissions, such as 'this.*containernode*.*physicalnode*'

Note that a specific node could be in both maps, if permissions like 'this.is.physicalnode' and 'this.is.physicalnode.end' were extant.