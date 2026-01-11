# NEI Enchantments (Fabric 1.21.x)

![License](https://img.shields.io/github/license/Berkan/nei)
![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.0--1.21.11-blue)
![Platform](https://img.shields.io/badge/Platform-Fabric-red)

A lightweight and powerful enchantment utility mod for Minecraft 1.21.x (1.21.0 to 1.21.11+), inspired by the classic **Not Enough Items (NEI)** 'X' key feature. It allows players in Creative mode to quickly enchant items with a modern and intuitive GUI.

---

## 🚀 Features

- **Quick Access:** Open the enchantment menu by pressing the **'X'** key while hovering over any item in your inventory.
- **Smart Filtering:** The menu automatically lists only the enchantments compatible with the item you are holding.
- **Dynamic Levels:** Go beyond vanilla limits! Apply enchantments up to **Level 10**.
- **Real-time Sync:** Changes are synchronized instantly between the visual slot and your player inventory. No more item duplication!
- **Modern GUI:** Clean, scrollable list with search-ready sorting and easy-to-use +/- level controls.
- **English & Turkish Support:** Full localization for both languages.

## ✨ Special Enchantments

This mod adds custom enchantments to enhance your gameplay:

- **Vein Miner (Damar Madenciliği):** 
  - Compatible with all **Pickaxes**.
  - Automatically mines the entire ore vein (up to 60 blocks) when a single ore block is broken.
  - Supports all vanilla ores and raw materials.

---

## ⌨️ Keybindings

- **[X]**: Open Enchantment Menu (Default, configurable in settings).
- **[+]**: Increase selected enchantment level.
- **[-]**: Decrease selected enchantment level.

---

## 🛠️ Installation

1. Make sure you have **[Fabric Loader](https://fabricmc.net/)** installed for **1.21.x** (supports 1.21.0 through 1.21.11+).
2. Drop the **Fabric API** jar into your `mods` folder.
3. Drop the **NEI Enchantments** jar into your `mods` folder.
4. Launch the game and enjoy!

## 📋 Version Compatibility

- **Minecraft:** 1.21.0 - 1.21.11+ (All 1.21.x versions)
- **Fabric Loader:** 0.16.0+
- **Java:** 21+
- **Fabric API:** Required (any 1.21.x compatible version)

## 🔒 Permissions

- **Creative Mode:** Required to use the enchantment features.
- **Server Safety:** Non-creative players cannot open the menu or apply enchantments through network packets.

---

## 🏗️ Building from Source

To build the project yourself, run the following command in the root directory:

```bash
./gradlew build
```

The compiled `.jar` file will be located in `build/libs/`.

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the [issues page](https://github.com/Berkan/nei/issues).

---

## 📜 License

Distributed under the **MIT License**. See `LICENSE` for more information.

---

### [TR] Özet

NEI Enchantments, klasik NEI modundaki 'X' tuşu büyüleme özelliğini modern Minecraft (1.21.1) sürümüne taşır. 
- **Damar Madenciliği** gibi özel büyüler içerir.
- Gelişmiş ve türkçe dil destekli GUI.
- Gereksiz eşya kopyalama (dupe) sorunları tamamen giderilmiştir.

## Development

This mod uses:
- Fabric Loom for building
- Yarn mappings
- Mixin for hooking into HandledScreen
- Fabric Networking API for client-server communication

## License

MIT License - See LICENSE file for details.
