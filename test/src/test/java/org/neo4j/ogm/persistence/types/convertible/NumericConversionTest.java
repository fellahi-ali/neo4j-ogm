/*
 * Copyright (c) 2002-2018 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 *  conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.persistence.types.convertible;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.domain.social.Immortal;
import org.neo4j.ogm.domain.social.Individual;
import org.neo4j.ogm.exception.core.MappingException;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;

/**
 * @author Luanne Misquitta
 */
public class NumericConversionTest extends MultiDriverTestClass {

    private Session session;

    @Before
    public void init() throws IOException {
        session = new SessionFactory(driver, "org.neo4j.ogm.domain.social").openSession();
    }

    @After
    public void destroy() {
        session.purgeDatabase();
    }

    /**
     * @see DATAGRAPH-600
     */
    @Test
    public void shouldSaveAndRetrieveNumbers() {
        Individual individual = new Individual();
        individual.setName("Gary");
        individual.setAge(36);
        individual.setBankBalance(99.99f);
        individual.setCode((byte) 10);
        individual.setNumberOfPets(301);
        individual.setNumberOfShoes((byte) 101);
        individual.setDistanceFromZoo(215.50f);
        individual.setFavouriteRadioStations(new Vector<Double>(Arrays.asList(97.4, 105.4, 98.2)));
        individual.primitiveFloatArray = new float[] { 5.5f, 6.6f };
        individual.setPrimitiveByteArray(new byte[] { 1, 2, 3, 4, 5 });
        individual.floatArray = new Float[] { Float.valueOf(1.1f), Float.valueOf(2.2f) };
        individual.integerArray = new Integer[] { Integer.valueOf(1000), Integer.valueOf(2000), Integer.valueOf(3000) };
        individual.integerCollection = Arrays.asList(Integer.valueOf(100), Integer.valueOf(200));
        individual.setFloatCollection(Arrays.asList(Float.valueOf(10.5f), Float.valueOf(20.5f), Float.valueOf(30.5f)));
        individual.setByteCollection(Arrays.asList(Byte.valueOf("1"), Byte.valueOf("2")));
        session.save(individual);

        session.clear();

        individual = session.load(Individual.class, individual.getId());
        assertThat(individual.getName()).isEqualTo("Gary");
        assertThat(individual.getAge()).isEqualTo(36);
        assertThat(individual.getBankBalance()).isEqualTo(99.99f, within(0f));
        assertThat(individual.getCode()).isEqualTo((byte) 10);
        assertThat(individual.getNumberOfPets()).isEqualTo(Integer.valueOf(301));
        assertThat(individual.getNumberOfShoes()).isEqualTo(Byte.valueOf((byte) 101));
        assertThat(individual.getDistanceFromZoo()).isEqualTo(Float.valueOf(215.50f));
        assertThat(individual.getFavouriteRadioStations()).hasSize(3);
        assertThat(individual.getFavouriteRadioStations()).contains(97.4, 98.2, 105.4);
        assertThat(individual.primitiveFloatArray).hasSize(2);
        assertThat(individual.getPrimitiveByteArray()).hasSize(5);
        assertThat(individual.floatArray).hasSize(2);
        assertThat(individual.integerArray).hasSize(3);
        assertThat(individual.integerCollection).hasSize(2);
        assertThat(individual.getFloatCollection()).hasSize(3);
        assertThat(individual.getByteCollection()).hasSize(2);
    }

    /**
     * @see DATAGRAPH-600
     */
    @Test(expected = MappingException.class)
    public void shouldFailForPrimitiveIntOverflow() {
        session.query("CREATE (i:Individual {name: 'Gary', age:" + Integer.MAX_VALUE + 1 + "})", Collections.EMPTY_MAP);
        session.loadAll(Individual.class).iterator().next();
    }

    /**
     * @see DATAGRAPH-600
     */
    @Test(expected = MappingException.class)
    public void shouldFailForPrimitiveFloatOverflow() {
        session
            .query("CREATE (i:Individual {name: 'Gary', bankBalance:" + Double.MAX_VALUE + "})", Collections.EMPTY_MAP);
        session.loadAll(Individual.class).iterator().next();
    }

    /**
     * @see DATAGRAPH-600
     */
    @Test(expected = MappingException.class)
    public void shouldFailForPrimitiveByteOverflow() {
        session.query("CREATE (i:Individual {name: 'Gary', code:" + Byte.MAX_VALUE + 1 + "})", Collections.EMPTY_MAP);
        session.loadAll(Individual.class).iterator().next();
    }

    /**
     * @see DATAGRAPH-600
     */
    @Test(expected = MappingException.class)
    public void shouldFailForIntegerOverflow() {
        session.query("CREATE (i:Individual {name: 'Gary', numberOfPets:" + Integer.MAX_VALUE + 1 + "})",
            Collections.EMPTY_MAP);
        session.loadAll(Individual.class).iterator().next();
    }

    /**
     * @see DATAGRAPH-600
     */
    @Test(expected = MappingException.class)
    public void shouldFailForFloatOverflow() {
        session.query("CREATE (i:Individual {name: 'Gary', distanceFromZoo:" + Double.MAX_VALUE + "})",
            Collections.EMPTY_MAP);
        session.loadAll(Individual.class).iterator().next();
    }

    /**
     * @see DATAGRAPH-600
     */
    @Test(expected = MappingException.class)
    public void shouldFailForByteOverflow() {
        session.query("CREATE (i:Individual {name: 'Gary', numberOfShoes:" + Byte.MAX_VALUE + 1 + "})",
            Collections.EMPTY_MAP);
        session.loadAll(Individual.class).iterator().next();
    }

    /**
     * @see DATAGRAPH-600
     */
    @Test(expected = MappingException.class)
    public void shouldFailForByteAsFloat() {
        session.query("CREATE (i:Individual {name: 'Gary', numberOfShoes: 3.5})", Collections.EMPTY_MAP);
        session.loadAll(Individual.class).iterator().next();
    }

    /**
     * @see DATAGRAPH-658
     */
    @Test
    public void shouldLoadDoubleWhenDecimalIsMissing() {
        session.query("CREATE (i:Individual {name: 'Gary', maxTemp: 31})", Collections.EMPTY_MAP);
        Individual i = session.loadAll(Individual.class).iterator().next();
        assertThat(i.getMaxTemp()).isEqualTo(new Double(31));
    }

    /**
     * @see DATAGRAPH-840
     */
    @Test
    public void shouldConvertToLongInsteadOfCasting() {
        Individual individual = new Individual();
        individual.setLongCollection(Arrays.<Long>asList(1l, 2l, 3l));
        session.save(individual);

        session.clear();
        individual = session.load(Individual.class, individual.getId());
        for (Number number : individual.getLongCollection()) {
            assertThat(number instanceof Long).isTrue();
        }
    }

    @Test
    public void shouldMapInitializedLongList() throws Exception {

        Immortal immortal = new Immortal("John", "Doe");
        session.save(immortal);

        session.clear();

        Result result = session.query("MATCH (m:Immortal) WHERE ID(m) = {id} RETURN m",
            Collections.singletonMap("id", immortal.getId()));

        assertThat(result).isNotNull();
    }
}
