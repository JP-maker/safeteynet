package com.openclassroom.safetynet.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ConvertToUpper {

    private static final Logger logger = LoggerFactory.getLogger(ConvertToUpper.class);

    public static List<String> convertList(List<String> listToConvert) {

        logger.info("Liste originale : {}", listToConvert);

        listToConvert.replaceAll(String::toUpperCase);

        logger.info("Liste modifiée (en majuscules) : {}", listToConvert);

        return listToConvert;
    }
}