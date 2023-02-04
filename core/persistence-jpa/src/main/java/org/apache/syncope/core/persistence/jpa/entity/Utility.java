package org.apache.syncope.core.persistence.jpa.entity;

import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;

public class Utility {

    public AnyUtils getAu(UserDAO userDAO, GroupDAO groupDAO, AnyObjectDAO anyObjectDAO){
        EntityFactory entityFactory = new JPAEntityFactory();
        System.out.println("Qui1");
        AnyUtils au = new JPAAnyUtils(userDAO, groupDAO, anyObjectDAO, entityFactory, AnyTypeKind.USER, false);
        System.out.println("Qui2");

        return au;
    }
}