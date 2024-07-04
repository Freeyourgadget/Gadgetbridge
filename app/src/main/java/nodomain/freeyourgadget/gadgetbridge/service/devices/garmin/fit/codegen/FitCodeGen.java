package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.GlobalFITMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionFileType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionGoalSource;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionGoalType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionLanguage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionMeasurementSystem;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionSleepStage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionWeatherAqi;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionWeatherCondition;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

// This class is only used to generate code, and will not be packaged in the final apk
@RequiresApi(api = Build.VERSION_CODES.O)
public class FitCodeGen {
    public static void main(final String[] args) throws Exception {
        new FitCodeGen().generate();
    }

    public void generate() throws IOException {
        final File factoryFile = new File("app/src/main/java/nodomain/freeyourgadget/gadgetbridge/service/devices/garmin/fit/messages/FitRecordDataFactory.java");

        final StringBuilder sbFactory = new StringBuilder();
        String header = getHeader(factoryFile);
        if (!header.isEmpty()) {
            sbFactory.append(header);
            sbFactory.append("\n");
        }

        sbFactory.append("package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;\n");
        sbFactory.append("\n");
        sbFactory.append("import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;\n");
        sbFactory.append("import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;\n");
        sbFactory.append("import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;\n");
        sbFactory.append("\n");
        sbFactory.append("//\n");
        sbFactory.append("// WARNING: This class was auto-generated, please avoid modifying it directly.\n");
        sbFactory.append("// See ").append(getClass().getCanonicalName()).append("\n");
        sbFactory.append("//\n");
        sbFactory.append("public class FitRecordDataFactory {\n");
        sbFactory.append("    private FitRecordDataFactory() {\n");
        sbFactory.append("        // use create\n");
        sbFactory.append("    }\n");
        sbFactory.append("\n");
        sbFactory.append("    public static RecordData create(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {\n");
        sbFactory.append("        switch (recordDefinition.getGlobalFITMessage().getNumber()) {\n");

        final ArrayList<GlobalFITMessage> globalFITMessages = new ArrayList<>(GlobalFITMessage.KNOWN_MESSAGES.values());
        Collections.sort(globalFITMessages, Comparator.comparingInt(GlobalFITMessage::getNumber));

        for (final GlobalFITMessage value : globalFITMessages) {
            final String className = "Fit" + capitalize(toCamelCase(value.name()));
            sbFactory.append("            case ").append(value.getNumber()).append(":\n");
            sbFactory.append("                return new ").append(className).append("(recordDefinition, recordHeader);\n");

            process(value);
        }

        sbFactory.append("        }\n");
        sbFactory.append("\n");
        sbFactory.append("        return new RecordData(recordDefinition, recordHeader);\n");
        sbFactory.append("    }\n");
        sbFactory.append("}\n");

        FileUtils.copyStringToFile(sbFactory.toString(), factoryFile, "replace");
    }

    public void process(final GlobalFITMessage globalFITMessage) throws IOException {
        final String className = "Fit" + capitalize(toCamelCase(globalFITMessage.name()));
        final File outputFile = new File("app/src/main/java/nodomain/freeyourgadget/gadgetbridge/service/devices/garmin/fit/messages/" + className + ".java");

        final List<String> imports = new ArrayList<>();
        imports.add(Nullable.class.getCanonicalName());
        imports.add(RecordData.class.getCanonicalName());
        imports.add(RecordDefinition.class.getCanonicalName());
        imports.add(RecordHeader.class.getCanonicalName());
        //imports.add(GBToStringBuilder.class.getCanonicalName());
        imports.addAll(getImports(outputFile));

        Collections.sort(imports);

        final Set<String> uniqueImports =new LinkedHashSet<>(imports);

        for (final GlobalFITMessage.FieldDefinitionPrimitive primitive : globalFITMessage.getFieldDefinitionPrimitives()) {
            final Class<?> fieldType = getFieldType(primitive);
            if (!Objects.requireNonNull(fieldType.getCanonicalName()).startsWith("java.lang")) {
                imports.add(fieldType.getCanonicalName());
            }
        }

        final StringBuilder sb = new StringBuilder();
        String header = getHeader(outputFile);
        if (!header.isEmpty()) {
            sb.append(header);
            sb.append("\n");
        }

        sb.append("package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;");
        sb.append("\n");

        sb.append("\n");
        boolean anyImport = false;
        for (final String i : uniqueImports) {
            if (i.startsWith("androidx")) {
                sb.append("import ").append(i).append(";\n");
                anyImport = true;
            }
        }

        if (anyImport) {
            sb.append("\n");
            anyImport = false;
        }
        for (final String i : uniqueImports) {
            if (i.startsWith("java.")) {
                sb.append("import ").append(i).append(";\n");
                anyImport = true;
            }
        }

        if (anyImport) {
            sb.append("\n");
            anyImport = false;
        }
        for (final String i : uniqueImports) {
            if (i.startsWith("nodomain.freeyourgadget") && !i.startsWith("java.")) {
                sb.append("import ").append(i).append(";\n");
                anyImport = true;
            }
        }

        if (anyImport) {
            sb.append("\n");
            anyImport = false;
        }
        for (final String i : uniqueImports) {
            if (!i.startsWith("androidx") && !i.startsWith("nodomain.freeyourgadget") && !i.startsWith("java.")) {
                sb.append("import ").append(i).append(";\n");
                anyImport = true;
            }
        }

        if (anyImport) {
            sb.append("\n");
        }
        sb.append("//\n");
        sb.append("// WARNING: This class was auto-generated, please avoid modifying it directly.\n");
        sb.append("// See ").append(getClass().getCanonicalName()).append("\n");
        sb.append("//\n");
        sb.append("public class ").append(className).append(" extends RecordData {\n");
        sb.append("    public ").append(className).append("(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {\n");
        sb.append("        super(recordDefinition, recordHeader);\n");
        sb.append("\n");
        sb.append("        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();\n");
        sb.append("        if (globalNumber != ").append(globalFITMessage.getNumber()).append(") {\n");
        sb.append("            throw new IllegalArgumentException(\"").append(className).append(" expects global messages of \" + ").append(globalFITMessage.getNumber()).append(" + \", got \" + globalNumber);\n");
        sb.append("        }\n");
        sb.append("    }\n");

        for (final GlobalFITMessage.FieldDefinitionPrimitive primitive : globalFITMessage.getFieldDefinitionPrimitives()) {
            final Class<?> fieldType = getFieldType(primitive);
            final String fieldTypeName = fieldType.getSimpleName();
            sb.append("\n");
            sb.append("    @Nullable\n");
            sb.append("    public ").append(fieldTypeName).append(method(" get", primitive)).append("() {\n");
            sb.append("        return (").append(fieldTypeName).append(") getFieldByNumber(").append(primitive.getNumber()).append(");\n");
            sb.append("    }\n");
        }

        //sb.append("\n");
        //sb.append("    @NonNull\n");
        //sb.append("    @Override\n");
        //sb.append("    public String toString() {\n");
        //sb.append("        return new GBToStringBuilder(this)\n");
        //for (final GlobalFITMessage.FieldDefinitionPrimitive primitive : globalFITMessage.getFieldDefinitionPrimitives()) {
        //    sb.append("                .append(\"").append(primitive.getName()).append("\",").append(method(" get", primitive)).append("())\n");
        //}
        //sb.append("                .build();\n");
        //sb.append("    }\n");

        if (outputFile.exists()) {
            // Keep manual changes if any
            final String fileContents = new String(Files.readAllBytes(outputFile.toPath()), StandardCharsets.UTF_8);
            final int manualChangesIndex = fileContents.indexOf("// manual changes below");
            if (manualChangesIndex > 0) {
                sb.append("\n");
                sb.append("    ");
                sb.append(fileContents.substring(manualChangesIndex));
            } else {
                sb.append("}\n");
            }
        } else {
            sb.append("}\n");
        }

        FileUtils.copyStringToFile(sb.toString(), outputFile, "replace");
    }

    public Class<?> getFieldType(final GlobalFITMessage.FieldDefinitionPrimitive primitive) {
        if (primitive.getType() != null) {
            switch (primitive.getType()) {
                case ALARM:
                    return Calendar.class;
                case DAY_OF_WEEK:
                    return DayOfWeek.class;
                case FILE_TYPE:
                    return FieldDefinitionFileType.Type.class;
                case GOAL_SOURCE:
                    return FieldDefinitionGoalSource.Source.class;
                case GOAL_TYPE:
                    return FieldDefinitionGoalType.Type.class;
                case MEASUREMENT_SYSTEM:
                    return FieldDefinitionMeasurementSystem.Type.class;
                case TEMPERATURE:
                    return Integer.class;
                case TIMESTAMP:
                    return Long.class;
                case WEATHER_CONDITION:
                    return FieldDefinitionWeatherCondition.Condition.class;
                case LANGUAGE:
                    return FieldDefinitionLanguage.Language.class;
                case SLEEP_STAGE:
                    return FieldDefinitionSleepStage.SleepStage.class;
                case WEATHER_AQI:
                    return FieldDefinitionWeatherAqi.AQI_LEVELS.class;
            }

            throw new RuntimeException("Unknown field type " + primitive.getType());
        }

        switch (primitive.getBaseType()) {
            case ENUM:
            case SINT8:
            case UINT8:
            case SINT16:
            case UINT16:
            case UINT8Z:
            case UINT16Z:
            case BASE_TYPE_BYTE:
                return Integer.class;
            case SINT32:
            case UINT32:
            case UINT32Z:
            case SINT64:
            case UINT64:
            case UINT64Z:
                return Long.class;
            case STRING:
                return String.class;
            case FLOAT32:
                return Float.class;
            case FLOAT64:
                return Double.class;
        }

        throw new RuntimeException("Unknown base type " + primitive.getBaseType());
    }

    public String toCamelCase(final String str) {
        final StringBuilder sb = new StringBuilder(str.toLowerCase());

        for (int i = 0; i < sb.length(); i++) {
            if (sb.charAt(i) == '_') {
                sb.deleteCharAt(i);
                sb.replace(i, i + 1, String.valueOf(Character.toUpperCase(sb.charAt(i))));
            }
        }

        return sb.toString();
    }

    public String method(final String methodName, final GlobalFITMessage.FieldDefinitionPrimitive primitive) {
        return methodName + capitalize(toCamelCase(primitive.getName()));
    }

    public String capitalize(final String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public String getHeader(final File file) throws IOException {
        if (file.exists()) {
            final String fileContents = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            final int packageIndex = fileContents.indexOf("package") - 1;
            if (packageIndex > 0) {
                return fileContents.substring(0, packageIndex);
            }
        }

        return "";
    }

    public List<String> getImports(final File file) throws IOException {
        if (file.exists()) {
            final String fileContents = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            final List<String> imports = new ArrayList<>();

            final Matcher m = Pattern.compile("import (.+);")
                    .matcher(fileContents);
            while (m.find()) {
                imports.add(m.group(1));
            }
            return imports;
        }

        return Collections.emptyList();
    }
}
