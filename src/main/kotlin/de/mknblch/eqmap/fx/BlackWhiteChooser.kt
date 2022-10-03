package de.mknblch.eqmap.fx

import de.mknblch.eqmap.common.ColorTransformer.Companion.generateMonochromePalette

internal class BlackWhiteChooser(size: Int = 12) : ColorChooser(colors = generateMonochromePalette(size)) {

}