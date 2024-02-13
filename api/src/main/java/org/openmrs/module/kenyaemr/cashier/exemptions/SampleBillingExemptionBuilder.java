package org.openmrs.module.kenyaemr.cashier.exemptions;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.openmrs.api.context.Context;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * Builds a list of exemptions from json file
 */
public class SampleBillingExemptionBuilder extends BillingExemptions {

    public SampleBillingExemptionBuilder() {
    }

    /**
     * It is good practice to adopt a convention that will make it easy to read the config.
     * You can prefix all program lists with Program, and age based lists with Age
     */
    @Override
    public void buildExemptionList() {
        String configurationFilePath = "/opt/tomcat/.OpenMRS/billing/billingConfig.json";
        FileInputStream fileInputStream;
        ObjectNode config = null;
        try {
            fileInputStream = new FileInputStream(configurationFilePath);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        if (fileInputStream != null) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                config = mapper.readValue(fileInputStream, ObjectNode.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (config != null) {
            ObjectNode configuredServices = (ObjectNode) config.get("services");
            ObjectNode commodities = (ObjectNode) config.get("commodities");

            if (configuredServices != null) {
                Map<String, Set<Integer>> exemptedServices = mapConcepts(configuredServices);
                BillingExemptions.setSERVICES(exemptedServices);
            }

            if (commodities != null) {
                Map<String, Set<Integer>> exemptedCommodities = mapConcepts(commodities);
                BillingExemptions.setCOMMODITIES(exemptedCommodities);
            }
        }
    }

    /**
     * Organize concepts to a map
     * @param node
     * @return
     */
    private Map<String, Set<Integer>> mapConcepts(ObjectNode node) {
        Map<String, Set<Integer>> exemptionList = new HashMap<String, Set<Integer>>();
        if (node != null) {
            Iterator<Map.Entry<String, JsonNode>> iterator = node.getFields();
            iterator.forEachRemaining(entry -> {
                Set<Integer> conceptSet = new HashSet<>();
                String key = entry.getKey();
                ArrayNode conceptIds = (ArrayNode) entry.getValue();
                if (conceptIds.isArray() && conceptIds.size() > 0) {
                    for (int i = 0; i < conceptIds.size(); i++) {
                        conceptSet.add((conceptIds.get(i).getIntValue()));
                    }
                }
                if (conceptSet.size() > 0) {
                    exemptionList.put(key, conceptSet);
                }
            });
        }
        return exemptionList;
    }
}
