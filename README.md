# NEI Enchantments (Fabric 1.21.11)

![License](https://img.shields.io/github/license/Berkan/nei)
![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.11-blue)
![Platform](https://img.shields.io/badge/Platform-Fabric-red)
![Version](https://img.shields.io/badge/Version-2.0.0-green)

A powerful enchantment utility mod for Minecraft 1.21.11, inspired by the classic **Not Enough Items (NEI)** 'X' key feature. It allows players in **any game mode** (Survival, Hardcore, Creative) to quickly enchant items with a modern and intuitive GUI.

---

## 🚀 Features

- **🎮 All Game Modes:** Works in Survival, Hardcore, and Creative modes!
- **⌨️ Quick Access:** Open the enchantment menu by pressing the **'X'** key while hovering over any item in your inventory.
- **🔍 Smart Filtering:** The menu automatically lists only the enchantments compatible with the item you are holding.
- **📈 Dynamic Levels:** Go beyond vanilla limits! Apply enchantments up to **Level 10**.
- **⚡ Real-time Sync:** Changes are synchronized instantly. No item duplication!
- **🎨 Modern GUI:** Clean, scrollable list with level controls and visual feedback.
- **🌍 Multi-language Support:** English and Turkish localizations included.
- **💬 Welcome Message:** Helpful tip on first join showing how to use the mod.

## ✨ Special Enchantments

This mod adds custom enchantments to enhance your gameplay:

- **Vein Miner (Damar Madenciliği):** 
  - Compatible with all **Pickaxes, Shovels, and Axes**.
  - Automatically mines entire veins or groups of connected blocks.
  - Range increases with level (up to 60 blocks at level 10).
  - Supports all vanilla ores, raw materials, dirt, gravel, sand, logs, and more.

---

## ⌨️ Keybindings & Usage

1. **Open Inventory:** Press **E**
2. **Hover over Item:** Move your mouse over the item you want to enchant
3. **Open Enchant Menu:** Press **X**
4. **Select Enchantment:** Click on an enchantment from the list
5. **Adjust Level:** Use **+** and **-** buttons (1-10)
6. **Apply:** Click the **Apply** button
7. **Remove:** Click the **Remove** button to remove an enchantment

---

## 🛠️ Installation

1. Make sure you have **[Fabric Loader](https://fabricmc.net/)** 0.18.4+ installed for **1.21.11**
2. Download **[Fabric API](https://modrinth.com/mod/fabric-api)** 0.141.1+1.21.11
3. Drop both jars into your `mods` folder
4. Drop the **NEI Enchantments** jar into your `mods` folder
5. Launch the game and enjoy!

## 📋 Requirements

- **Minecraft:** 1.21.11
- **Fabric Loader:** 0.18.4+
- **Fabric API:** 0.141.1+1.21.11
- **Java:** 21+
## 📝 Changelog

### Version 2.0.0 (Latest)
- ✅ **Full Survival and Hardcore mode support** - No longer Creative-only!
- ✅ **Improved slot detection** - Mouse-based system works in all game modes
- ✅ **Enhanced GUI** - Visible inventory slots with proper backgrounds
- ✅ **Level display** - See current selected level clearly
- ✅ **Welcome message** - Helpful tip on first join
- ✅ **Item return fix** - Items no longer disappear when closing GUI
- ✅ **Bug fixes** - Resolved empty slot detection and coordinate issues
- ✅ **Performance improvements** - Cleaner code and optimized rendering

### Version 1.0.0
- Initial release for Minecraft 1.21.11
- Creative mode enchantment system
- Vein Miner custom enchantment
- Basic GUI with scrolling support
- English and Turkish localization

---

## 🏗️ Building from Source

To build the project yourself:

```bash
git clone https://github.com/yourusername/nei-enchantments.git
cd nei-enchantments
gradlew build
```

The compiled `.jar` file will be located in `build/libs/nei-enchantments-2.0.0.jar`

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the [issues page](https://github.com/yourusername/nei-enchantments/issues).

---

## 📜 License

Distributed under the **MIT License**. See `LICENSE` for more information.

---

## 👨‍💻 Developer

**Berkan** - [GitHub](https://github.com/yourusername)

---

### [TR] Özet

NEI Enchantments, klasik NEI modundaki 'X' tuşu büyüleme özelliğini modern Minecraft 1.21.11 sürümüne taşır. 
- **Tüm oyun modlarında çalışır:** Survival, Hardcore ve Creative
- **Damar Madenciliği** gibi özel büyüler içerir
- Gelişmiş ve türkçe dil destekli GUI
- Gereksiz eşya kopyalama (dupe) sorunları tamamen giderilmiştir
- Envanter açıkken item üzerine gelip X tuşuna basarak kullanılır

This mod uses:
- Fabric Loom for building
- Yarn mappings
- Mixin for hooking into HandledScreen
- Fabric Networking API for client-server communication

## License

MIT License - See LICENSE file for details.
