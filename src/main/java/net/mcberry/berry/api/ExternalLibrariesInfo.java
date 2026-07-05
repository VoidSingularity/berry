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

import net.mcberry.berry.loader.ExternalLibraryCollection;

public class ExternalLibrariesInfo extends ExternalLibraryCollection {
    public ExternalLibrariesInfo() {
        lib("ada2141c0cc52ee8f5c48cd5fa4ce0e794f22236", "https://repo1.maven.org/maven2/org/ow2/asm/asm/9.10.1/asm-9.10.1.jar");
        lib("8d49f14d51f632cb1d87c88d1ceaf50db0d8af1b", "https://repo1.maven.org/maven2/org/ow2/asm/asm-analysis/9.10.1/asm-analysis-9.10.1.jar");
        lib("4229e4c55fd8e01c23f9fe9884075cc628aacc50", "https://repo1.maven.org/maven2/org/ow2/asm/asm-commons/9.10.1/asm-commons-9.10.1.jar");
        lib("e244332a17564c1d1572449399a842de35881be2", "https://repo1.maven.org/maven2/org/ow2/asm/asm-tree/9.10.1/asm-tree-9.10.1.jar");
        lib("7bb9d450e8d4cbf9f9e04096c44bbfe7fba80b15", "https://repo1.maven.org/maven2/org/ow2/asm/asm-util/9.10.1/asm-util-9.10.1.jar");
        lib("41c4a3984a80f4679e759fb9f495587acc5cdac7", "https://repo1.maven.org/maven2/net/fabricmc/sponge-mixin/0.17.3+mixin.0.8.7/sponge-mixin-0.17.3+mixin.0.8.7.jar");
        lib("0626e00b72e3879a07e6653d8015cd3466ff5b75", "https://repo1.maven.org/maven2/io/github/llamalad7/mixinextras-common/0.5.4/mixinextras-common-0.5.4.jar");
    }
}
