**Alfresco Max Version Policy**

Author: Jared Ottley (*jared.ottley@alfresco.com*)  
Date: 2/20/2018  
Version: 0.0.10 

**Summary**  
Alfresco Max Version Policy limits the number of versions that are created for a versioned node. The default value is set for 10 versions per node. This can be overridden by setting maxVersions=<value> in alfresco-global.properties file.

**Install**  
Alfresco Max Version Policy is delivered as an AMP (https://github.com/jottley/alfresco-maxversion-policy/releases/download/0.0.10/alfresco-max-version-policy-0.0.10.amp).  Copy the AMP into the amps directory and run the apply_amps.[sh|bat] script. Alfresco must not be running when you apply the AMP.

Alfresco Minimum Version required: 4.2.0  

**Contributions**  
* Konst Sergeev <https://github.com/ksergeev> - Allow disabling the policy by setting maxVersions to zero
* Konst Sergeev <https://github.com/ksergeev> - Clean long history of legacy nodes completely
