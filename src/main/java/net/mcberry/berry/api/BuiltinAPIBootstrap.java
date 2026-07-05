// Copyright (C) 2025 VoidSingularity

// This program is free software: you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or (at
// your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.

// You should have received a copy of the GNU Lesser General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package net.mcberry.berry.api;

import java.io.IOException;
import java.util.List;

import net.mcberry.berry.api.asm.AccessTransformer;
import net.mcberry.berry.api.mixins.MixinInitialize;
import net.mcberry.berry.loader.classloading.BerryClassLoader;
import net.mcberry.berry.loader.BerryLoader;
import net.mcberry.berry.loader.BerryModInitializer;
import net.mcberry.berry.loader.ExternalLibraryCollection;
import net.mcberry.berry.loader.JarContainer;
import net.mcberry.berry.utils.StringSorter;

public class BuiltinAPIBootstrap implements BerryModInitializer {
    private static JarContainer container;
    private static ExternalLibraryCollection elc;

    public static JarContainer getContainer() {
        return container;
    }

    @Override
    public void preinit(StringSorter sorter, JarContainer jar, String name) {
        sorter.addValue(name);
        container = jar;
        elc = new ExternalLibrariesInfo();
        elc.initialize();
    }

    private static final List<String> services = List.of(
            "org.spongepowered.asm.service.IGlobalPropertyService",
            "org.spongepowered.asm.service.IMixinService",
            "org.spongepowered.asm.service.IMixinServiceBootstrap"
    );

    public void initialize(String[] argv) {
        // Import ELC
        elc.imports();

        // Get URL for services
        try {
            for (var service : services)
                BerryClassLoader.getInstance().controlResources(
                        "META-INF/services/" + service,
                        BerryClassLoader.toURL("META-INF/services/" + service, container.filepath())
                );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Mixin bootstrap
        MixinInitialize.initialize();
        BerryLoader.preloaders.add(cl -> MixinInitialize.addcfg());

        // AT bootstrap
        AccessTransformer.init();
        try {
            var resources = this.getClass().getClassLoader().getResources("META-INF/berry.at");
            while (resources.hasMoreElements()) {
                var url = resources.nextElement();
                var stream = url.openStream();
                AccessTransformer.loadstream("berry", stream);
                stream.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Add the libraries on server side
        if (BerryLoader.getSide().equals("SERVER")) {
            System.out.println("[BERRY] Extracting server libraries...");
            try {
                ServerLibraryLoader.load();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
