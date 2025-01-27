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

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

public class BerryGlobalPropertyService implements IGlobalPropertyService {
    public static record MixinStringPropertyKey (String key) implements IPropertyKey {}
    private final Map <String, Object> properties = new HashMap <> ();
    @Override public IPropertyKey resolveKey (String name) { return new MixinStringPropertyKey (name); }
    @SuppressWarnings ("unchecked") @Override public <T> T getProperty (IPropertyKey key) { return (T) properties.get (((MixinStringPropertyKey) key) .key); }
    @SuppressWarnings ("unchecked") @Override public <T> T getProperty (IPropertyKey key, T fb) { return (T) properties.getOrDefault (((MixinStringPropertyKey) key) .key, fb); }
    @Override public void setProperty (IPropertyKey key, Object value) { properties.put (((MixinStringPropertyKey) key) .key, value); }
    @Override public String getPropertyString (IPropertyKey key, String fb) {
        Object o = properties.get (((MixinStringPropertyKey) key) .key);
        return o == null ? fb : o.toString ();
    }
}
