package de.orb.wiiu.rpxgadgetfinder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import de.orb.wiiu.rpxparser.ElfSection;
import de.orb.wiiu.rpxparser.RPXFile;
import lombok.val;

public class App {
    private static final String OPTION_HELP = "help";
    private static final String OPTION_CONFIG_IN_LONG = "config_input";
    private static final String OPTION_CONFIG_IN = "cin";
    private static final String OPTION_BINARY_IN_LONG = "binary_input";
    private static final String OPTION_BINARY_IN = "bin";
    private static final String OPTION_ADDRESS_OFFSET_LONG = "address_offset";
    private static final String OPTION_ADDRESS_OFFSET = "aoff";

    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        Options options = getOptions();

        if (args.length == 0) {
            showHelp(options);
            return;
        }

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e1) {
            System.err.println(e1.getMessage());
            showHelp(options);
            return;
        }

        if (cmd.hasOption(OPTION_HELP)) {
            showHelp(options);
            return;
        }

        String configFilename = cmd.getOptionValue(OPTION_CONFIG_IN);
        String rpxrplFilename = cmd.getOptionValue(OPTION_BINARY_IN);
        long addr_offset = 0;
        if (cmd.hasOption(OPTION_ADDRESS_OFFSET_LONG)) {
            addr_offset = Long.parseLong(cmd.getOptionValue(OPTION_ADDRESS_OFFSET_LONG));
        }

        File configFile = new File(configFilename);
        if (!configFile.exists()) {
            System.err.println(configFile.getAbsolutePath() + " does not exist.");
            System.exit(-1);
        }

        Yaml yaml = new Yaml(new Constructor(SymbolList.class));
        SymbolList curSymbolList = null;

        try {
            curSymbolList = yaml.load(new FileInputStream(configFile));
        } catch (Exception e) {
            System.err.println("Failed to parse config: " + configFile.getAbsolutePath() + ". " + e.getMessage());
            System.exit(-1);
        }

        long totalOffset = addr_offset;
        String path = rpxrplFilename.replace('/', File.separatorChar);
        File rawRPX = new File(path);
        if (!rawRPX.exists()) {
            System.err.println("WARNING: File doesn't exist: " + rawRPX.getAbsolutePath());
            System.exit(-1);
        }
        RPXFile rpx = null;
        try {
            rpx = new RPXFile(rawRPX);
        } catch (IOException e1) {
            System.err.println("Failed to parse rpx: " + rawRPX.getAbsolutePath());
            e1.printStackTrace();
            System.exit(-1);
        }
        val exports = rpx.getExports();

        val exportSymbols = curSymbolList.getSymbols().stream().filter(s -> s instanceof ExportSymbol).map(s -> (ExportSymbol) s).collect(Collectors.toList());
        val gadgetSymbols = curSymbolList.getSymbols().stream().filter(s -> s instanceof GadgetSymbol).map(s -> (GadgetSymbol) s).collect(Collectors.toList());

        ElfSection section = rpx.getTextSection().get();
        byte[] textSection = section.getSectionBuffer().array();

        for (val cur : gadgetSymbols) {
            byte[] buffer = new byte[cur.getSize()];
            boolean found = false;
            for (int i = 0; i < textSection.length - cur.getSize(); i += 4) {
                System.arraycopy(textSection, i, buffer, 0, cur.getSize());
                byte[] hash = MessageDigest.getInstance("SHA-256").digest(buffer);
                if (Arrays.equals(hash, cur.getHash())) {
                    System.out.println(String.format("%s = 0x%08X;", cur.getOut(), i + section.address() + totalOffset));
                    found = true;
                    break;
                }
            }
            if (!found) {
                System.err.println(String.format("Not found %s", cur.toString()));
            }
        }

        for (val s : exportSymbols) {
            val opt = exports.stream().filter(e -> e.name().equals(s.getName())).findAny();
            if(opt.isPresent()) {
                System.out.println(String.format("%s = 0x%08X;", s.getOut(), opt.get().offset() + totalOffset));
            }else {
                System.err.println(String.format("Not found %s", s.toString()));
            }
        }
    }
    
    /*
    private static void createDefaultFile() throws IOException {
        SymbolList coreinit = new SymbolList();

        coreinit.add(new ExportSymbol("memcpy", "$ROP_memcpy"));
        coreinit.add(new ExportSymbol("DCFlushRange", "$ROP_DCFlushRange"));
        coreinit.add(new ExportSymbol("ICInvalidateRange", "$ROP_ICInvalidateRange"));
        coreinit.add(new ExportSymbol("OSSwitchSecCodeGenMode", "$ROP_OSSwitchSecCodeGenMode"));
        coreinit.add(new ExportSymbol("OSCodegenCopy", "$ROP_OSCodegenCopy"));
        coreinit.add(new ExportSymbol("OSGetCodegenVirtAddrRange", "$ROP_OSGetCodegenVirtAddrRange"));
        coreinit.add(new ExportSymbol("OSGetCoreId", "$ROP_OSGetCoreId"));
        coreinit.add(new ExportSymbol("OSGetCurrentThread", "$ROP_OSGetCurrentThread"));
        coreinit.add(new ExportSymbol("OSSetThreadAffinity", "$ROP_OSSetThreadAffinity"));
        coreinit.add(new ExportSymbol("OSYieldThread", "$ROP_OSYieldThread"));
        coreinit.add(new ExportSymbol("OSFatal", "$ROP_OSFatal"));
        coreinit.add(new ExportSymbol("_Exit", "$ROP_Exit"));
        coreinit.add(new ExportSymbol("OSScreenFlipBuffersEx", "$ROP_OSScreenFlipBuffersEx"));
        coreinit.add(new ExportSymbol("OSScreenClearBufferEx", "$ROP_OSScreenClearBufferEx"));
        coreinit.add(new ExportSymbol("OSDynLoad_Acquire", "$ROP_OSDynLoad_Acquire"));
        coreinit.add(new ExportSymbol("OSDynLoad_FindExport", "$ROP_OSDynLoad_FindExport"));
        coreinit.add(new ExportSymbol("__os_snprintf", "$ROP_os_snprintf"));
        coreinit.add(new GadgetSymbol("c87020ec5098d13edd3ee0d0d01313a0a5f0a7937f36c0f5f4e9503165ae33fb", 0x10, "$ROP_POPJUMPLR_STACK12"));
        coreinit.add(new GadgetSymbol("decff3ca875efc1a9c3d0ac7618f3efa3d33ca59bd3fd602a747d3469bd5c000", 0x10, "$ROP_POPJUMPLR_STACK20"));
        coreinit.add(new GadgetSymbol("5306248821c072a9cf5c71c9469171e17cd2966f66fc7d612ce79cff8d5d124a", 0x34, "$ROP_CALLFUNC"));
        coreinit.add(new GadgetSymbol("f4e76053a65c571f2b3b8c6c6dde973c95f889bccf0a22eb9649cbc7810c8e98", 0x2c, "$ROP_CALLR28_POP_R28_TO_R31"));
        coreinit.add(new GadgetSymbol("4741b863adcf742f8928c485a39f3cc8629469d1ddccf9e68c9dd1c25341f091", 0x20, "$ROP_POP_R28R29R30R31"));
        coreinit.add(new GadgetSymbol("972973be8074e92b0f10fc5fbbaaef6c28e2905f99007654cd735a5fd69933fc", 0x14, "$ROP_POP_R27"));
        coreinit.add(new GadgetSymbol("6f5f11fdc441ddef8f2189cbc9006517c4917fcf6e975cb8cbeb2373bf8e8ca2", 0x14, "$ROP_POP_R24_TO_R31"));
        coreinit.add(new GadgetSymbol("e602f66cf8aadc4d5e7db074ad9b8fbfa4190f862a8215cf26f707a49ca95070", 0x28, "$ROP_CALLFUNCPTR_WITHARGS_FROM_R3MEM"));
        coreinit.add(new GadgetSymbol("5e1fb4810ff6fb86bb533f2050306de6e03e09450887df6cb22c6d8511c3e267", 0x18, "$ROP_SETR3TOR31_POP_R31"));
        coreinit.add(new GadgetSymbol("5CED182718E8204C299EA1F8E295841A0325EE493893B86053DE762CC0EEFB48", 0x0C, "$ROP_Register"));
        coreinit.add(new GadgetSymbol("C457C33CF42B00C2E00B96E2C6B097848643BC172E8BDC9F0E7D974E833860B6", 0x0C, "$ROP_CopyToSaveArea"));

        Yaml yaml = new Yaml(new Constructor(SymbolList.class));
        Writer writer = new FileWriter(new File("coreinit.yml"));
        yaml.dump(coreinit, writer);

        SymbolList gx2 = new SymbolList();
      
        gx2.add(new ExportSymbol("GX2Init", "$ROP_GX2Init"));
        gx2.add(new ExportSymbol("GX2Flush", "$ROP_GX2Flush"));
        gx2.add(new ExportSymbol("GX2DirectCallDisplayList", "$ROP_GX2DirectCallDisplayList"));

        writer.close();
        writer = new FileWriter(new File("gx2.yml"));
        yaml.dump(gx2, writer);
        writer.close();
    }
    */

    private static Options getOptions() {
        Options options = new Options();

        options.addOption(Option.builder(OPTION_CONFIG_IN).longOpt(OPTION_CONFIG_IN_LONG).required().hasArg().argName("filename").desc("path to a config.yml")
                .required().build());
        options.addOption(Option.builder(OPTION_BINARY_IN).longOpt(OPTION_BINARY_IN_LONG).required().hasArg().argName("filename").desc("path to the rpx/rpl that should be parsed")
                .required().build());
        options.addOption(Option.builder(OPTION_ADDRESS_OFFSET).longOpt(OPTION_ADDRESS_OFFSET_LONG).required().hasArg().argName("offset").desc("offset that will be added to the addresses")
                .required().build());

        options.addOption(OPTION_HELP, false, "shows this text");

        return options;
    }

    private static void showHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(100);
        formatter.printHelp(" ", options);
    }
}
