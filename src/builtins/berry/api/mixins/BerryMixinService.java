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

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.LoggerAdapterDefault;
import org.spongepowered.asm.mixin.MixinEnvironment.CompatibilityLevel;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.IMixinInternal;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.util.ReEntranceLock;

import berry.api.BuiltinAPIBootstrap;
import berry.loader.BerryClassTransformer;
import berry.loader.BerryLoader;

public class BerryMixinService implements IMixinService, IClassProvider, IClassBytecodeProvider {
    public static ClassNode getClassNode (String name, int flags) throws IOException {
        String slash = name.replace ('.', '/');
        InputStream stream = BuiltinAPIBootstrap.class.getClassLoader () .getResourceAsStream (slash + ".class");
        byte[] data = stream.readAllBytes ();
        stream.close ();
        data = BerryClassTransformer.instance () .remap (null, slash, null, null, data);
        ClassNode node = new ClassNode ();
        ClassReader reader = new ClassReader (data);
        reader.accept (node, flags);
        return node;
    }
    @Override public ClassNode getClassNode (String name, boolean rt, int flags) throws ClassNotFoundException, IOException { return getClassNode (name, flags); }
    @Override public ClassNode getClassNode (String name) throws ClassNotFoundException, IOException { return this.getClassNode (name, true); }
    @Override public ClassNode getClassNode (String name, boolean rt) throws ClassNotFoundException, IOException { return this.getClassNode (name, rt, 0); }
    /* As it is deprecated in 0.8 */ @Override public URL[] getClassPath () { return new URL [0]; }
    @Override public Class <?> findClass (String name) throws ClassNotFoundException { return Class.forName (name); }
    @Override public Class <?> findClass (String name, boolean initialize) throws ClassNotFoundException { return Class.forName (name, initialize, this.getClass () .getClassLoader ()); }
    @Override public Class <?> findAgentClass (String name, boolean initialize) throws ClassNotFoundException { return findClass (name, initialize); }
    @Override public String getName () { return "BerryLoader"; }
	@Override public boolean isValid () { return true; }
    @Override public void prepare () {}
    @Override public Phase getInitialPhase () { return Phase.INIT; }
    static IMixinTransformer transformer;
    @Override public void offer (IMixinInternal internal) {
        if (internal instanceof IMixinTransformerFactory factory) {
            transformer = factory.createTransformer ();
        }
    }
    @Override public void init () {}
    @Override public void beginPhase () {}
    @Override public void checkEnv (Object bs) {}
    private final ReEntranceLock lock = new ReEntranceLock (1);
    @Override public ReEntranceLock getReEntranceLock () { return lock; }
    @Override public IClassProvider getClassProvider () { return this; }
    @Override public IClassBytecodeProvider getBytecodeProvider () { return this; }
    @Override public ITransformerProvider getTransformerProvider () { return null; }
    @Override public IClassTracker getClassTracker () { return null; }
    @Override public IMixinAuditTrail getAuditTrail () { return null; }
    @Override public Collection <String> getPlatformAgents () { return List.of ("org.spongepowered.asm.launch.platform.MixinPlatformAgentDefault"); }
    @Override public IContainerHandle getPrimaryContainer () {
        try { return new ContainerHandleURI (this.getClass () .getProtectionDomain () .getCodeSource () .getLocation () .toURI ()); }
        catch (URISyntaxException e) { throw new RuntimeException (e); }
    }
    @Override public Collection <IContainerHandle> getMixinContainers () { return List.of (); }
    @Override public InputStream getResourceAsStream (String name) { return this.getClass () .getClassLoader () .getResourceAsStream (name); }
    @Override public String getSideName () { return BerryLoader.getSide (); }
    // TODO: Fix compatibility level
    @Override public CompatibilityLevel getMinCompatibilityLevel () { return CompatibilityLevel.JAVA_8; }
    @Override public CompatibilityLevel getMaxCompatibilityLevel () { return CompatibilityLevel.JAVA_22; }
    private static final Map <String, ILogger> loggers = new HashMap <> ();
    @Override public ILogger getLogger (String name) {
        if (loggers.containsKey (name)) return loggers.get (name);
        ILogger nl = new LoggerAdapterDefault (name);
        loggers.put (name, nl);
        return nl;
    }
}
