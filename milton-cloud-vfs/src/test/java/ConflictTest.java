/*
 * Copyright 2013 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author brad
 */
public class ConflictTest {

    SessionFactory sessionFactory;
    Session session;

    @Before
    public void setUp() {
//
//        AnnotationConfiguration configuration = new AnnotationConfiguration();
//        configuration.addAnnotatedClass(Repository.class)
//                .addAnnotatedClass(Branch.class)
//                .addAnnotatedClass(Commit.class)
//                .addAnnotatedClass(FanoutEntry.class)
//                .addAnnotatedClass(FanoutHash.class)
//                .addAnnotatedClass(BlobHash.class)
//                ;
//        configuration.setProperty("hibernate.dialect","org.hibernate.dialect.H2Dialect");
//        configuration.setProperty("hibernate.connection.driver_class","org.h2.Driver");
//        configuration.setProperty("hibernate.connection.url", "jdbc:h2:mem");
//        configuration.setProperty("hibernate.hbm2ddl.auto", "create");
//
//        sessionFactory = configuration.buildSessionFactory();
//        session = sessionFactory.openSession();

    }

    @After
    public void tearDown() {
    }

    @Test
    public void hello() {
        System.out.println("hello");
    }
}