package dev.ucdm.dataset.cli.nccopy

import dev.ucdm.core.write.Netcdf3FormatWriter
import dev.ucdm.core.write.NetcdfFileFormat
import dev.ucdm.dataset.api.CdmDatasets
import dev.ucdm.core.write.NetcdfCopier
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import java.io.IOException

/*
Usage: nccopy options_list
Options:
    --input, -in -> Input dataset to copy (always required) { String }
    --output, -out -> Output file to write to (always required) { String }
    --isLargeFile, -large [false] -> Write to Netcdf3 large file offset format.
    --help, -h -> Usage info
 */
fun main(args: Array<String>) {
    val parser = ArgParser("nccopy")
    val input by parser.option(
        ArgType.String,
        shortName = "in",
        description = "Input dataset to copy"
    ).required()
    val output by parser.option(
        ArgType.String,
        shortName = "out",
        description = "Output file to write to"
    ).required()
    val isLargeFile by parser.option(
        ArgType.Boolean,
        shortName = "large",
        description = "Write to Netcdf3 large file offset format."
    ).default(false)

    parser.parse(args)

    println("nccopy $input to $output")
    if (input == output) {
        println("ERROR: input '$input' cannot equal output '$output'")
        return
    }

    try {
        val ncwriter = Netcdf3FormatWriter.createNewNetcdf3(output)
        if (isLargeFile) {
            ncwriter.setFormat(NetcdfFileFormat.NETCDF3_64BIT_OFFSET)
        }

        CdmDatasets.openDataset(input).use { ncfile ->
            NetcdfCopier.create(ncfile, ncwriter).use { copier -> copier.write(null) }
        }

    } catch (ioe: IOException) {
        println("ERROR: '${ioe.message}'")
    }
}