/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.kenyaemr.cashier.exemptions;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;

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
     */
    @Override
    public void buildBillingExemptionList() {
        AdministrationService adminService = Context.getAdministrationService();
        String exemptionConfig = adminService.getGlobalProperty("kenyaemr.billing.exemptions");
        ObjectNode config = null;

        if (exemptionConfig != null) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                config = (ObjectNode) mapper.readTree(exemptionConfig);
            } catch (IOException e) {
                e.printStackTrace();
                initializeExemptionsConfig();
                System.out.println("The configuration file for billing exemptions was found, but could not be understood. Check that the JSON object is well formed");
                return;
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
        } else {
            initializeExemptionsConfig();
        }
    }

    /**
     * Maps exemption list in maps for faster access
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
                        ObjectNode conceptObj = (ObjectNode) conceptIds.get(i);
                        Integer conceptId = conceptObj.get("concept") != null ? conceptObj.get("concept").getIntValue() : null;
                        if (conceptId != null) {
                            conceptSet.add((conceptId));
                        }
                    }
                }
                if (conceptSet.size() > 0) {
                    exemptionList.put(key, conceptSet);
                }
            });
        }
        return exemptionList;
    }

    /**
     * Setting these to empty sets which are easy to work with than nulls
     */
    private void initializeExemptionsConfig() {
        BillingExemptions.setCOMMODITIES(new HashMap<>());
        BillingExemptions.setSERVICES(new HashMap<>());
    }
}
