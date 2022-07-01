package dev.cdm.dataset.coordsysbuild

import dev.cdm.dataset.api.CdmDataset

open class GiefConventions(name: String = "GIEF") : CoordSysBuilder(name) {

    override fun augment(dataset: CdmDataset) : CdmDataset {
        return GiefAugment(dataset, this.info).augment()
    }

}