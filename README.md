# MaceLimit (container-ban verze)

Zakazuje vložení vybraných itemů do jakéhokoliv kontejneru (truhla, sud,
shulker box, ender chest...) - jak kliknutím, tak tažením myší (drag), a to
i když jsou schované uvnitř shulker boxu nebo bundle.

## Co je blokováno

V `MaceLimit.java`, metoda `isBlocked()`:
- `Material.MACE`
- itemy, jejichž display name je v `BLOCKED_NAMES` (teď: Copper Helmet/
  Chestplate/Leggings/Boots/Pickaxe)

Uprav `BLOCKED_NAMES` nebo přidej další `Material` podmínku podle potřeby -
žádné jiné nastavení není potřeba.

## Co bylo z původní verze odstraněno

Tahle verze dělá JEN blokování kontejnerů. Odstraněno bylo:
- limit 1 mace na server (crafting lock + sledování zničení)
- limit 3 totemů v inventáři
- zákaz ender perel
- mazání vanilla receptů (copper armor/tools, Crafter blok)
- všechny broadcast zprávy do chatu

Pokud budeš chtít něco z tohohle zpátky, klidně napiš.
