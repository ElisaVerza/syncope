package org.apache.syncope.core.provisioning.java.utils;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.GuardedByteArray;


@RunWith(value=Parameterized.class)
public class ConnObjectUtilsTest {
    enum ObjectType {
        GUARDEDSTRING,
        GUARDEDBYTEARRAY,
        STRING,
        OTHER
    }

    private GuardedString gsPass;
    private GuardedByteArray gbaPass;
    private String strPass;
    private String passToWrite = "Test";
    private Object objPass;
    private ConnObjectUtils cou;
    private ObjectType actuaObj;

    @Mock
    TemplateUtils templateUtils;
    @Mock
    RealmDAO realmDAO;
    @Mock
    UserDAO userDAO;
    @Mock
    ExternalResourceDAO resourceDAO;
    @Mock
    PasswordGenerator passwordGenerator;
    @Mock
    MappingManager mappingManager;
    @Mock
    AnyUtilsFactory anyUtilsFactory;


    @Parameters
    public static Collection<Object[]> getTestParameters(){
        return Arrays.asList(new Object[][]{     
            {ObjectType.GUARDEDSTRING},
        });
    }

    public ConnObjectUtilsTest(ObjectType actuaObj){
        this.actuaObj = actuaObj;
    }

    @Before
    public void getPassSetUp(){
        cou = new ConnObjectUtils(templateUtils, realmDAO, userDAO, resourceDAO, passwordGenerator, mappingManager, anyUtilsFactory);
        switch(actuaObj) {
            case GUARDEDSTRING:
                char[] passwordToChar = passToWrite.toCharArray();
                gsPass = new GuardedString(passwordToChar);
                objPass = gsPass;
              break;
            case GUARDEDBYTEARRAY:
                byte[] passwordToByte = passToWrite.getBytes();
                gbaPass = new GuardedByteArray(passwordTpByte);
                break;
            case STRING:
                objPass = passToWrite;
              break;
            default:

        }
    }


    @Test
    public void dummyTest(){
        assertEquals("Test", ConnObjectUtils.getPassword(objPass));
    }
}
