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

import org.spongepowered.asm.service.IMixinServiceBootstrap;

public class BerryMixinServiceBootstrap implements IMixinServiceBootstrap {
    @Override public String getName () { return "Berry"; }
    @Override public String getServiceClassName () { return BerryMixinService.class.getName (); }
    @Override public void bootstrap () {  }
}
