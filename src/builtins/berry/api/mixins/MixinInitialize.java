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

package berry.api.mixins;

import java.lang.reflect.Method;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;

import berry.loader.BerryClassTransformer;
import berry.utils.Graph;

public class MixinInitialize {
    public static void initialize () {
        MixinBootstrap.init ();
        try {
			Method m = MixinEnvironment.class.getDeclaredMethod ("gotoPhase", MixinEnvironment.Phase.class);
			m.setAccessible (true);
			m.invoke (null, MixinEnvironment.Phase.INIT);
			m.invoke (null, MixinEnvironment.Phase.DEFAULT);
		} catch (Exception e) {
			throw new RuntimeException (e);
		}
        // hello world
        Mixins.addConfiguration ("builtin_mixins.json");
        BerryClassTransformer.ByteCodeTransformer transformer = (loader, name, clazz, domain, code) -> {
            try {
                name = name.replace ('/', '.');
                return BerryMixinService.transformer.transformClassBytes (name, name, code);
            } catch (Throwable t) {
                System.err.println (String.format ("[BERRY/MIXIN] Error transforming class %s", name));
                t.printStackTrace ();
                return null;
            }
        };
        BerryClassTransformer.instance () .all.addVertex (new Graph.Vertex ("berry::mixin", transformer));
        MixinExtrasBootstrap.init ();
    }
}
