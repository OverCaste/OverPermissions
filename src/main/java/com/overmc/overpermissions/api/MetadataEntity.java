package com.overmc.overpermissions.api;

public interface MetadataEntity {
    /**
     * Checks both the global metadata store and the world-specific store for a single entry.
     * 
     * @param key the key for which to check for a value.
     * @param worldName the name of the world for this metadata to be checked in.
     * @return true if this entity has that specific metadata value set, false otherwise.
     * 
     * @see #getMeta(String, String)
     * @see #setMeta(String, String, String)
     */
    public boolean hasMeta(String key, String worldName);

    /**
     * Check to see if this entity's global metadata store holds an entry for that key.
     * 
     * @param key the key of the metadata to be checked for.
     * @return whether or not this entity has that specific metadata value set.
     */
    public boolean hasGlobalMeta(String key);

    /**
     * Retrieve a metadata value from a world store, or if it doesn't exist, from the global store.
     * 
     * @param key the key from which to retrieve the value from.
     * @param worldName the name of the world for the value to be retrieved from.
     * @return the string representation of the metadata value at the specified key.
     * 
     * @see #setMeta(String, String, String)
     * @see #hasMeta(String, String)
     */
    public String getMeta(String key, String worldName);

    /**
     * @param key the key from which to retrieve the value from.
     * @return the string representation of the metadata value at the specified key.
     */
    public String getGlobalMeta(String key);

    /**
     * Sets a single metadata entry. If you want to set multiple metadata values at once, please use {@link #setBatchMeta(MetadataBatch)}.
     * 
     * @param key the key at which the metadata value should be set.
     * @param value the value of the metadata.
     * @param worldName the name of the world in which the metadata value will be set.
     */
    public void setMeta(String key, String value, String worldName);

    /**
     * Sets a single metadata entry in the global store. If you want to set multiple metadata values at once, please use {@link #setBatchMeta(MetadataBatch)}.
     * 
     * @param key the key at which the metadata value should be set.
     * @param value the value of the metadata.
     */
    public void setGlobalMeta(String key, String value);

    /**
     * Removes a single metadata entry from the pool of a specified world.
     * 
     * @param key the key at which the metadata value should be removed.
     * @param worldName the name of the world in which the metadata entry will be removed.
     * @return whether or not there was a valid metadata entry at that key.
     * 
     * @see #setBatchMeta(MetadataBatch)
     * @see #getMeta(String, String)
     * @see #hasMeta(String, String)
     * @see #removeGlobalMeta(String)
     */
    public boolean removeMeta(String key, String worldName);

    /**
     * Removes a single global metadata entry from the global pool.
     * 
     * @param key the key at which the metadata value should be removed.
     * @return whether or not there was a valid metadata entry at that key.
     */
    public boolean removeGlobalMeta(String key);

    /**
     * Sets a batch of metadata key-value pairs.
     * 
     * @param batch the batch to be set
     * @return true if all key-value pairs were changed, or false otherwise.
     * 
     * @see MetadataBatch
     */
    public boolean setBatchMeta(MetadataBatch batch);

    /**
     * @return the entire batch of metadata that represents this entity.
     */
    public MetadataBatch getAllMetadata( );
}
