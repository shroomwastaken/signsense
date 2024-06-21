package com.signsense.app.analysis;

/**
 * A class which contains a list of dictionaries to translate signs to
 */
public final class SignDictionary {
    private SignDictionary() {}

    /**
     * Each dictionary is made for each model version (1-4)
     */
    public static final String[][] RU_DACTYL = {
        {"а","б","в","г","д","е","ё","ж","з","и","й","к","л","м","н","о","п","р","с","т","у","ф","х","ц","ч","ш","щ","ъ","ы","ь","э","ю","я"},
        {"а","б","в","г","д","е","ё","ж","з","[и/й]","к","л","м","н","о","п","р","с","т","у","ф","х","ц","ч","[ш/щ]","ъ","ы","ь","э","ю","я"},
        {"а","б","в","г","д","[е/ё]","ж","з","[и/й]","к","л","м","н","о","п","р","с","т","у","ф","х","ц","ч","[ш/щ]","ъ","ы","ь","э","ю","я"},
        {"а","б","в","г","д","[е/ё]","ж","з","[и/й]","к","л","м","н","о","п","р","с","т","у","ф","х","ц","ч","[ш/щ]","ъ","ы","ь","э","ю","я"}
    };
}