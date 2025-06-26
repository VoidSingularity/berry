# Berry Loader

Berry Loader is a mod loader for Minecraft, designed for extreme extensibility.

## Build and Development

In order to build this project, you must have Python 3.8+ installed.
The default java is `java`. If this java does not satisfy Minecraft's requirements, then
you have to configure java path yourself. Create a JSON file `localinfo.json`, then write
```
{
    "java": ".../bin/"
}
```
where `...` is your java's path, and the config value ends with `bin/`. `localinfo.json` also contains some other configurations.

To execute a task, run `build.py <task>`. For example,
run `build.py run_client` should launch Minecraft client after downloading mandatory assets and libraries.

If you want to work on this project with Visual Studio Code, run `build.py setup_vscode`. If you are using Intellij IDEA, run `build.py setup_intellij`.

## `localinfo.json` Configuration

You can use `localinfo.json` to configure this project.

`java`: This value's usage is described above.

`auth_name` and `auth_uuid`:
You can use these two values to configure the username and UUID when developing Minecraft.
This does not allow you to visit online-mode servers. The only things it can do is to change
your username and skin.

`dist_addr` and `dist_pswd`:
These two values are for [dist.py](https://github.com/azure-bluet/dist.py) configurations.
[dist.py](https://github.com/azure-bluet/dist.py) allows you to share built resources
between different mod workspaces without having to upload them publicly. This feature
requires an accessible server (e.g. a local server accessible from LAN).
If a `dist.py` environment is setup but you want to disable it temporarily, run
`build.py xxxx --offline`.

## License and credits

**SPECIAL NOTICE: To work with this project, you agree with Mojang's [End User License Agreement](https://www.minecraft.net/en-us/eula). If you do not agree, you may not download
resources from Minecraft.**

Files in this repository are licensed under the GNU Lesser General Public License, unless otherwise specified.

The buildscript templates (`build.py` and `project_template.py`) are licensed under GNU General Public License. However,
if you are using them for development, you do NOT have to publish your own mod under these terms, as the buildscripts do NOT
exist at runtime/in production.
Another template file `mapping.py` is licensed under MIT license.

**Note:**
some code comes from other places. See the copyright notice in those files for more info.

SpecialSource (libs/specialsource.jar) is authored by md_5 and has its own license (see libs/LICENSE-SpecialSource.md).

**Special credits:**

Zetaser_jtz for supporting this project.
