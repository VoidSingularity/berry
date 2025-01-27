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

package berry.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import berry.api.mixins.MixinInitialize;
import berry.loader.BerryLoader;
import berry.loader.BerryModInitializer;
import berry.loader.JarContainer;

public class BuiltinAPIBootstrap implements BerryModInitializer {
    public void initialize (String[] argv) {
        // Load bundled
        for (String jarname : JarContainer.containers.keySet ()) {
            JarContainer jar = JarContainer.containers.get (jarname);
            var file = jar.file ();
            var entry = file.getEntry ("META-INF/bundled_jars");
            if (entry != null) {
                try (InputStream is = file.getInputStream (entry); Scanner scanner = new Scanner (is)) {
                    while (scanner.hasNextLine ()) {
                        String line = scanner.nextLine () .strip ();
                        if (line.isEmpty ()) continue;
                        if (line.startsWith ("#")) continue;
                        BundledJar.addBundled (BerryLoader.getGameDirectory (), jar, line);
                    }
                } catch (IOException e) {
                    System.err.println ("[BERRY/BUILTIN] Unexpected failure while reading bundle info!");
                    e.printStackTrace ();
                }
            }
        }
        // Mixin bootstrap
        MixinInitialize.initialize ();
    }
}
