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

import net.mcberry.berry.loader.classloading.BerryClassLoader;
import net.mcberry.berry.utils.ReflectionUtil;
import net.minecraft.bundler.Main;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ServerLibraryLoader {
    public static void load() throws IOException, ReflectiveOperationException {
        var inst = new Main();
        readAndExtractDir(inst, "libraries");
        readAndExtractDir(inst, "versions");
    }
    private static void readAndExtractDir(Main inst, String subdir) throws IOException, ReflectiveOperationException {
        Method m1 = ReflectionUtil.getMethod(inst.getClass(), "checkAndExtractJar");
        Method m2 = ReflectionUtil.getMethod(inst.getClass(), "readResource");
        Method m3 = ReflectionUtil.getMethod(inst.getClass(), "lambda$readAndExtractDir$0");
        Class<?> entry = Class.forName("net.minecraft.bundler.Main$FileEntry");
        Class<?> parser = Class.forName("net.minecraft.bundler.Main$ResourceParser");
        Object proxy = Proxy.newProxyInstance(
                Main.class.getClassLoader(),
                new Class<?>[] { parser },
                (prox, meth, args) -> m3.invoke(null, args)
        );
        // A copy of the `readAndExtractJar` method
        List<?> entries = (List)m2.invoke(inst, subdir + ".list", proxy);
        Path subdirPath = Paths.get(System.getProperty("bundlerRepoDir", "")).resolve(subdir);
        Field pathof = ReflectionUtil.getField(entry, "path");
        for(Object obj : entries) {
            String pth = (String) pathof.get(obj);
            Path outputFile = subdirPath.resolve(pth);
            m1.invoke(inst, subdir, obj, outputFile);
            BerryClassLoader.getInstance().appendToClassPathForInstrumentation(outputFile.toString());
        }
    }
}
