# RealmEngine

_"Mo gives us convenience, but they also curse us with it being in JSON and C." - Anonymous user_

_"WE ARE SANE MOD DEVELOPERS" - Modoromu_

A library mod primarily made for Realmfall.

Most functionality seen here was originally hard-coded features in Realmfall, previously requiring Java code to implement.
RealmEngine is the JSON-ification of Realmfall's preivously Java-based features, allowing for more flexibility and
accessibility for modders and datapack makers alike, not just Realmfall developers. 
We've even included APIs such as our pausable potion effects for public use!

Datapacks are universal and are more proofed against up-ports or major point-version changes.

## Pausable Status Effects (Java)
Via the `IPausableEffect` interface, status effects can be paused and resumed at will. 

Example usage:
```java
public void applyEffectTick(LivingEntity entity, int tick) {
    ((IPausableEffect) entity.getEffect(this))
            .realmengine$setDurationPaused(entity.hasEffect(ModEffects.GENE_SHUFFLE_PAUSE.get()));
    super.applyEffectTick(entity, tick);
}
```
This pauses the duration countdown of the effect when the entity has the `GENE_SHUFFLE_PAUSE` effect, and resumes it when the entity no longer has that effect.

Credits: Nischi & Modoromu

## Bugfixes:

- PersistentEntitySectionManager: Fixed strange AIOOBE server crashes caused by other mods accessing data from off-threads by using unsafe multithreading.

## Armor Properties

A datapack-based system to apply set effects, randomized mods, and applicable armor modifiers to armors.
Designed to be as lightweight and accessible as possible, allowing any other mod or a vanilla datapack to utilize the features.

**WARNING:** Armor Properties are still in early development and their API is unstable, especially as we rewrite armor mods. What is NOT unstable is the JSON.

### Datapack Structure

```plaintext
data/
  my_namespace/
    armor_properties/
      <property_id>.json
```

### Documentation

Base JSON schema:
```json
{
  "id": "gold_armor", // Unique identifier for this property. Used for referencing in other properties, and for debugging purposes.
  "armor_items" : [ // List of armor items that this property applies to. Can be items from other mods, or even vanilla items. Each item can have a corresponding modset, which determines the random mods that can be applied to that item.
    {
      "item_id": "minecraft:golden_helmet", // The item ID of the armor piece to apply this property to. Can be from other mods, or even vanilla items.
      "modset" : "bisccel:helmet_set"       // Modset to apply to this armor piece.
    },
    {
      "item_id": "minecraft:golden_chestplate",
      "modset" : "bisccel:chestplate_set"
    },
    {
      "item_id": "minecraft:golden_leggings",
      "modset" : "bisccel:leggings_set"
    },
    {
      "item_id": "minecraft:golden_boots",
      "modset" : "bisccel:boots_set"
    } // Good practice to have at most 4 pieces of armor in one property, especially if you want to apply set effects. Make sure it consists of a full set of armor worn in the default 4 slots, otherwise the set effects won't be applied.
  ],
  "random_mod_count" : 2, // Number of mods to apply from the modset in an armor piece, randomly selected. Optional, defaults to 0.
  "applicable_armor_mods" : [ // Optional. If specified, these armor mods will be applied to the armor when equipped, in addition to the random mods from the modset. Can be left empty if no applicable armor mods are desired.
                              // THIS REQUIRES JAVA CODING TO IMPLEMENT THE ARMOR MODS THEMSELVES. Create an item using the ItemApplicableArmorMod class!
    {
      "id": "bisccel:heat_resist_armor_mod",  // Resource location for this armor mod item.
      "optional" : true,
      "slots" : [ // The armor slots that this mod can be applied to. Can be any combination of "HEAD", "CHEST", "LEGS", and "BOOTS".
        "CHEST",
        "LEGS"
      ],
      "mod_effects:" : [ // The effects that this mod provides when applied. Only supports attribute modifiers.
        {
          "name" : "gold_armor_heat_resist",
          "attribute_name" : "bisccel:max_temperature_tolerance",
          "modifier_operation" : "addition",
          "value" : 2.0
        }
      ]
    } //... Add more applicable armor mods as needed.
  ],
  "set_effects" : [ // Optional. If specified, these effects will be applied when the full set of armor is worn. Can be left empty if no set effects are desired.
    {
      "name" : "gold_armor_set_armor",
      "attribute_name" : "minecraft:generic.armor",
      "modifier_operation" : "addition",
      "value" : 1.0
    }
  ]
}
```

### Performance

Properties are supplied upon datapack reload and are stored in a map, allowing for quick retrieval when needed.
No notable change in datapack reload times have been observed. Memory overhead is too miniscule for it to matter.

Set effects are attribute modifiers applied via the `LivingEquipmentChangeEvent` subscriber.
In Forge's own words, _"LivingEquipmentChangeEvent is fired when the Equipment of an Entity changes.
This event is fired whenever changes in Equipment are detected in LivingEntity.tick().
This also includes entities joining the World, as well as being cloned.
This event is fired on server-side only."_

Armor modifiers are applied via the `ItemAttributeModifierEvent`, for both armor mods, and random gear mods.
Similar to the above, this event is fired when an item's attribute modifiers are being calculated, which is not
every tick, only when necessary (e.g. when the item is equipped or when the player's attributes are being recalculated).

In short: This is not fired every tick, only when an entity's equipment changes,
a living entity joins the world, or when a living entity is created/copied.

Compared to Realmfall's original system of checking **every tick on every entity** for armor sets and then applying effects
**every tick**, RealmEngine's system is significantly more efficient and should not cause any noticeable performance issues,
even on lower-end hardware. The bulk is only ran when players change their armor, which is a very infrequent action.

Therefore, if it causes significant overhead, **then Forge is doing something terribly wrong.**

## Modsets
A `modset` is a collection of mods that can be selected randomly and applied to gear.
Currently, it is used by `armor_properties` and `item_properties`.

### Datapack Structure

```plaintext
data/
  my_namespace/
    modsets/
      <modset_id>.json
```

### Documentation

Base JSON schema:
```json
{
  "id" : "example_boots_set",        // Unique identifier for this modset. Used for referencing in properties, and for debugging purposes.
  "replace" : false,                 // Optional. If true, this modset will replace any existing modset with the same ID. If false, this modset will only add to existing sets. Defaults to false.
  "boosted_stat_day" : "wednesday",  // Optional. If specified, the mods in this modset will have their stats boosted by the "boosted_stat_rate" field on the specified day. 
                                     // The day is determined by the server's local time, and is one of "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", or "saturday". If not specified, no stat boosting will occur.
  "boosted_stat_rate" : 1.5,         // Optional. The rate at which the mods in this modset will be boosted on the specified day. For example, if this is 1.5, then the mods will have their stats increased by 50% on the specified day. If not specified, defaults to 1 (no boost).
  "modifiers" : [
    {
      "name" : "mymod:boots_helmet_speed",  // Unique identifier for this modifier. Used for UUID.
      "attribute_name" : "minecraft:generic.movement_speed", // The attribute that this modifier modifies. Must be a valid attribute in the game.
      "modifier_operation" : "addition", // The operation to apply the modifier with. Can be "addition", "multiply_base", or "multiply_total".
      "min_value" : 0.01, // The value of the modifier will be randomly selected from this range when applied. Can be negative for debuffs.
      "max_value" : 0.02  // The value of the modifier will be randomly selected from this range when applied. Can be negative for debuffs.
    } // ... Add more modifiers as needed.
  ]
}
```

**Keep in mind that when making armor properties, it's a good practice to make separate modsets for each piece of armor,
even if the mods are the same, to avoid UUID conflicts.**
UUID conflicts can cause mods to not be applied correctly, or to be applied with the wrong values.

## Item Properties
A datapack-based system to apply set effects, randomized mods, and applicable modifiers to items.
Designed to be as lightweight and accessible as possible, allowing any other mod or a vanilla datapack to utilize the features.


### Datapack Structure

```plaintext
data/
  my_namespace/
    item_properties/
      <item_properties_id>.json
```

### Documentation

Base JSON schema:
```json
{
  "id": "one_mod_gear",                   // Unique identifier for this property. Used for referencing in other properties, and for debugging purposes.
  "items" : [                             // List of items that this property applies to. Can be items from other mods, or even vanilla items.
    {
      "item_id": "minecraft:golden_sword",// The item ID of the item to apply this property to. Can be from other mods, or even vanilla items.
      "modset" : "mymod:common_sword"     // Modset to apply to this item. Mods are applied randomly from the modset, and the number of mods applied is determined by the "random_mod_count" field.
    } // ... Add more items as needed. Keep in mind that one file applies up to random_mod_count mods to each item, so if you want different mods for different items, you need to create multiple properties.
  ],
  "random_mod_count" : 1                  // Number of mods to apply from the modset in an item, randomly selected. Optional, defaults to 0.
}
```

## Hammertime & Oversized Tools:
As of Realmengine v4.0, Hammertime has been merged into RealmEngine. 
While it is up to *individual mods* to implement the features of Hammertime, RealmEngine provides the core functionality for oversized tools and weapons.


### Adding Your Own Hammers
(Mod devs)

*This guide implies you already have a tool tier defined in your mod and item registration subscribed to.*

Adding your own hammers is quite easy, we'll start by defining our custom hammer in our item registries:
```java
public static final RegistryObject<Item>
        CUSTOM_HAMMER =  ITEMS.register("custom_hammer",  () -> RealmEngine.newHammer(Tiers.CUSTOM_TIER));
```
Alternatively, you can create a new hammer directly:
```java
public static final RegistryObject<Item>
        CUSTOM_HAMMER =  ITEMS.register("custom_hammer",  () -> new HammerItem(tier, 8,-2.9f, new Item.Properties().stacksTo(1), 0)); //Modify that last int if you want to go beyond 3x3 mining
```
It's recommended to handle these registries on a conditional statement that checks if 
***RealmEngine*** is loaded.

Now, it is registered into the game, but we aren't done yet! 
In your mod's datapack folder, create a folder named `weapon_attributes`, 
and create a file inside of it named `custom_hammer.json`, with the following contents:
```json
{
    "parent": "realmengine:hammer"
}
```
***Hammertime!*** provides a combo for Hammer-type weapons for Better Combat.

Once these are done, you can use datagen or manually create the item model 
in your mod's assets folder, using the `heldhammer` as a parent model.
```json
{
  "parent": "realmengine:item/heldhammer",
  "textures": {
    "layer0": "mymod:item/custom_hammer"
  }
}
```
Now, all that is left is a lang file entry, 
adding it to the `realmengine:hammers` tag (Which gets added to `minecraft:pickaxes` automatically), 
and adding in your texture. 
It's that easy and painless!

# Modpage description
## RealmEngine - The Core Library of Realmfall.
Disclaimer: RealmEngine doesn't do much out of the box, some datapack assembly required. Curios is required as compatibility with it is projected for 4.0 release.

"Slice the moon," said He of Hope, "and welcome the potential of the fallout."

A set of datapack-based features for both mod and modpack developers alike!
We at Team Biscuit maintain and develop RealmEngine primarily for our needs, but a lot we end up making modpack development much more accessible than before.
Therefore, we split the total game overhaul that is Realmfall into a library and the content mod.
RealmEngine is free to use, released for the whole of the Minecraft 1.20.1 modding community.
Our gift, to you.

Features:
- Configurable! Great for modpacks!
- Armor Mods (Improve your gear using custom-defined modifiers and items.)
- Random Gear Mods (Each piece of armor is uniquely stronger/weaker than its base form! WIP: curios, and tools/weapons)
- Armor Set Effects (Wearing a full set of armor provides additional benefits!)
- WIP: Metaworld (A simulation outside the simulation, a performant way to breathe life beyond the loaded chunks)
- Bugfixes (Fixes crashes caused by unsafe multithreading)
- Mob stat tiering (Not enabled by default as of 4.0)
- Hammers
- Pausable potion effects (Programmable!)

FastNoiseLite v1.1.1 is licensed under the MIT License and included in RealmEngine.