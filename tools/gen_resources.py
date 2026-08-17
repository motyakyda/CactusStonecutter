#!/usr/bin/env python3
import json, os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

MIX5 = ["AbstractClientPlayerEntityMixin", "MinecraftClientMixin", "OptionsScreenMixin",
        "PlayerListEntryMixin", "TitleScreenMixin"]
MIX4 = ["MinecraftClientMixin", "OptionsScreenMixin", "PlayerListEntryMixin", "TitleScreenMixin"]

TABLE = {
    "1.16.5": (8,  "JAVA_8",  MIX5),
    "1.17.1": (16, "JAVA_16", MIX5),
    "1.18.2": (17, "JAVA_17", MIX5),
    "1.19.2": (17, "JAVA_17", MIX5),
    "1.19.4": (17, "JAVA_17", MIX5),
    "1.20.2": (17, "JAVA_17", MIX4),
    "1.20.4": (17, "JAVA_17", MIX4),
    "1.20.6": (21, "JAVA_21", MIX4),
    "1.21.11":(21, "JAVA_21", MIX4),
}

def main():
    for ver, (java, compat, mixins) in TABLE.items():
        d = os.path.join(ROOT, "versions", ver, "src", "main", "resources")
        os.makedirs(d, exist_ok=True)
        mod = {
            "schemaVersion": 1,
            "id": "cactusskins",
            "version": "{{version}}",
            "name": "CactusSkins",
            "description": "Кастомные скины и плащи для всех игроков с модом. Кнопка Skins на главном экране.",
            "authors": ["you"],
            "license": "MIT",
            "environment": "client",
            "mixins": ["cactusskins.mixins.json"],
            "depends": {
                "fabricloader": ">=0.15.0",
                "minecraft": "~" + ver,
                "java": ">=" + str(java),
            },
        }
        mix = {
            "required": True,
            "package": "dev.cactusskins.mixin",
            "compatibilityLevel": compat,
            "client": mixins,
            "injectors": {"defaultRequire": 1},
        }
        with open(os.path.join(d, "fabric.mod.json"), "w", encoding="utf-8") as f:
            json.dump(mod, f, ensure_ascii=False, indent=2)
            f.write("\n")
        with open(os.path.join(d, "cactusskins.mixins.json"), "w", encoding="utf-8") as f:
            json.dump(mix, f, ensure_ascii=False, indent=2)
            f.write("\n")
        print("wrote", ver)

if __name__ == "__main__":
    main()
