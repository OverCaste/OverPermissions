#Method for handling UUIDs
##The example scenario:
 * player joins, name a, uuid 0
 * player joins, name b, uuid 1
 * player leaves, name a
 * player joins, name c, uuid 0

###At this location, the uuid map should be as follows:
 * a -> 0
 * c -> 0
 * b -> 1

##And when a player 'reclaims' a uuid that is 'stagnant' in the database:
 * player joins, name a, uuid 3

###The uuid map is updated to:
 * a -> 3
 * b -> 1
 * c -> 0
 
## For correct operation,  this method should be followed:
 Player joins -> set the value at (the player's username) to (the player's uuid)
 
 Player leaves -> do nothing

## And then to retrieve the uuid:
 * If the player's online, retrieve the UUID that way, as it's faster.
 * Otherwise, if the player exists in the persistent database or cache, retrieve the uuid there.
 * Finally, if no other locations are met, retrieve a new UUID and store/cache it as required.