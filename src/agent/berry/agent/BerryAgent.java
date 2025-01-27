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

package berry.agent;

import java.lang.instrument.Instrumentation;

import berry.loader.BerryClassTransformer;

public class BerryAgent {
    public static void premain (String arg, Instrumentation inst) {
        // Basically, how Berry Mod Loader works is like this:
        // The berryagent.jar is started through JVM arguments: -javaagent:berryagent.jar
        // The berrylib.jar and asm.jar is added to the classpath
        // Then the berryagent.jar will initialize berry.loader.BerryClassTransformer
        // The main class is changed into berry.loader.BerryLoaderMain
        // The main class will search for loader mods in the mods directory and load them
        // The loader mods should load the rest of the mods
        inst.addTransformer (new BerryClassTransformer (inst), true);
        System.out.println ("[JA] BerryAgent loaded.");
    }
}
