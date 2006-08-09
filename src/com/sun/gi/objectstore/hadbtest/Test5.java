/*
 * Copyright © 2006 Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, California 95054, U.S.A. All rights reserved.
 * 
 * Sun Microsystems, Inc. has intellectual property rights relating to
 * technology embodied in the product that is described in this
 * document. In particular, and without limitation, these intellectual
 * property rights may include one or more of the U.S. patents listed at
 * http://www.sun.com/patents and one or more additional patents or
 * pending patent applications in the U.S. and in other countries.
 * 
 * U.S. Government Rights - Commercial software. Government users are
 * subject to the Sun Microsystems, Inc. standard license agreement and
 * applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * 
 * Sun, Sun Microsystems, the Sun logo and Java are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other
 * countries.
 * 
 * This product is covered and controlled by U.S. Export Control laws
 * and may be subject to the export or import laws in other countries.
 * Nuclear, missile, chemical biological weapons or nuclear maritime end
 * uses or end users, whether direct or indirect, are strictly
 * prohibited. Export or reexport to countries subject to U.S. embargo
 * or to entities identified on U.S. export exclusion lists, including,
 * but not limited to, the denied persons and specially designated
 * nationals lists is strictly prohibited.
 * 
 * Copyright © 2006 Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, California 95054, Etats-Unis. Tous droits réservés.
 * 
 * Sun Microsystems, Inc. détient les droits de propriété intellectuels
 * relatifs à la technologie incorporée dans le produit qui est décrit
 * dans ce document. En particulier, et ce sans limitation, ces droits
 * de propriété intellectuelle peuvent inclure un ou plus des brevets
 * américains listés à l'adresse http://www.sun.com/patents et un ou les
 * brevets supplémentaires ou les applications de brevet en attente aux
 * Etats - Unis et dans les autres pays.
 * 
 * L'utilisation est soumise aux termes de la Licence.
 * 
 * Cette distribution peut comprendre des composants développés par des
 * tierces parties.
 * 
 * Sun, Sun Microsystems, le logo Sun et Java sont des marques de
 * fabrique ou des marques déposées de Sun Microsystems, Inc. aux
 * Etats-Unis et dans d'autres pays.
 * 
 * Ce produit est soumis à la législation américaine en matière de
 * contrôle des exportations et peut être soumis à la règlementation en
 * vigueur dans d'autres pays dans le domaine des exportations et
 * importations. Les utilisations, ou utilisateurs finaux, pour des
 * armes nucléaires,des missiles, des armes biologiques et chimiques ou
 * du nucléaire maritime, directement ou indirectement, sont strictement
 * interdites. Les exportations ou réexportations vers les pays sous
 * embargo américain, ou vers des entités figurant sur les listes
 * d'exclusion d'exportation américaines, y compris, mais de manière non
 * exhaustive, la liste de personnes qui font objet d'un ordre de ne pas
 * participer, d'une façon directe ou indirecte, aux exportations des
 * produits ou des services qui sont régis par la législation américaine
 * en matière de contrôle des exportations et la liste de ressortissants
 * spécifiquement désignés, sont rigoureusement interdites.
 */

/*
 * Copyright 2005, Sun Microsystems, Inc. All Rights Reserved. 
 */

package com.sun.gi.objectstore.hadbtest;

import com.sun.gi.objectstore.ObjectStore;
import com.sun.gi.objectstore.Transaction;

/**
 * @author Daniel Ellard
 */

public class Test5 {

    static public void main(String[] args) {
	long appID = 1;

	/*
	 * The params should depend on the args.  At present
	 * everything is hard-wired.
	 */

	if ((args.length != 0) && (args.length != 1)) {
	    System.out.println("incorrect usage");
	    System.exit(1);
	}

	TestParams params;
	if (args.length == 0) {
	    params = new TestParams();
	} else {
	    params = new TestParams(args[1]);
	}
	System.out.println(params.toString());

	/*
	 * Connect to the database, test the connection.
	 */

	ObjectStore os = TestUtil.connect(appID, true,
		params.dataSpaceType, params.traceFileName);
	// os.clear();

	if (TestUtil.sanityCheck(os, "Hello, World", true)) {
	    System.out.println("appears to work");
	}
	else {
	    System.out.println("yuck");
	    os.close();
	    return;
	}
	os.close();

	params.numObjs = 10000;

	for (int objSize = 1 * 1024; objSize <= 16 * 1024; objSize *= 4) {

	    os = TestUtil.connect(appID, true, params.dataSpaceType, null);
	    // os.clear();

	    /*
	     * Create a bunch of objects, and then chop them up into clusters.
	     */

	    params.objSize = objSize;
	    ObjectCreator creator = new ObjectCreator(os, 0, null);
	    long[] oids = creator.createNewBunch(params.numObjs, params.objSize,
		    1, true);

	    if (params.dataSpaceType.equals("persistant-inmem")) {
		TestUtil.snooze(10000, "waiting for the new objects to settle");
	    }

	    os.close();

	    lookupTest(appID, oids, params, 1, 1);
	    lookupTest(appID, oids, params, 100, 1);

	    accessTest(appID, oids, params, true, 1, 1);
	    accessTest(appID, oids, params, true, 100, 1);
	    accessTest(appID, oids, params, false, 1, 1);
	    accessTest(appID, oids, params, false, 100, 1);

	    long[][] clusters = makeClusters(oids, params);

	    for (int peeks = 2; peeks <= 10; peeks += 4) {
		for (int locks = 0; locks <= 10; locks += 10) {
		    params.transactionNumPeeks = peeks;
		    params.transactionNumLocks = locks;
		    params.transactionNumPromotedPeeks = 0;

		    transactionTest(appID, clusters, params);
		}
	    }
	}

	os = TestUtil.connect(appID, false, params.dataSpaceType, null);
	os.clear();
	os.close();
    }

    private static long[][] makeClusters(long[] oids, TestParams params) {
	long[][] clusters;

	try {
	    clusters = FakeAppUtil.createRelatedClusters(oids,
		    params.numObjs / params.clusterSize, params.clusterSize,
		    params.skipSize);
	}
	catch (Exception e) {
	    e.printStackTrace(System.out);
	    return null;
	}

	return clusters;
    }

    private static void transactionTest(long appID, long[][] clusters,
	    TestParams params)
    {

	System.out.println(params.toString());

	//System.out.println("transactionTest numObjs " + params.numObjs);
	//System.out.println("transactionTest clusterSize " + params.clusterSize);
	//System.out.println("transactionTest skipSize " + params.skipSize);

	for (long snooze = 0; snooze >= 0; snooze -= 1) {
	    if (snooze != 0) {
		System.out.println("!!! !!! !!! !!! NONZERO SNOOZE: " + snooze);
	    }

	    if (params.numThreads == 1) {
		System.out.println("starting one thread");
		ClientTest5 t = new ClientTest5(appID, 0, params,
			clusters, snooze, 2000);
		t.run();
	    } else {
		System.out.println("starting " + params.numThreads +
			" threads");

		ClientTest5[] clients = new ClientTest5[params.numThreads];
		Thread[] allThreads = new Thread[params.numThreads];
		
		for (int i = 0; i < params.numThreads; i++) {
		    clients[i] = new ClientTest5(appID, i, params,
			    clusters, snooze, 2000);
		    allThreads[i] = new Thread(clients[i]);
		    allThreads[i].start();
		}

		for (int i = 0; i < params.numThreads; i++) {
		    try {
			allThreads[i].join();
		    } catch (Exception e) {
			System.out.println("unexpected: " + e);
		    }
		}
	    }
	    if (params.dataSpaceType.equals("persistant-inmem")) {
		TestUtil.snooze(10000, "waiting for activity to drain");
	    }
	}
	System.out.println("done");
    }

    private static void accessTest(long appID, long[] oids, TestParams params,
	    boolean doLock, int opsPerTrans, int laps) {
	long startTime, endTime;
	Transaction trans = null;

	ObjectStore os = TestUtil.connect(appID, false,
		params.dataSpaceType, null);

	startTime = System.currentTimeMillis();

	int opsSeenInTrans = 0;
	trans = os.newTransaction(null);
	trans.start();

	for (int lap = 0; lap < laps; lap++) {
	    for (int i = 0; i < oids.length; i++) {

		try {
		    FillerObject fo;

		    if (doLock) {
			fo = (FillerObject) trans.lock(oids[i]); 
		    } else {
			fo = (FillerObject) trans.peek(oids[i]); 
		    }
		    if (fo.getOID() != oids[i]) {
			System.out.println("CHECK FAILED: " + fo.getOID() + " != " +
				oids[i]);
		    }
		} catch (Exception e) {
		    System.out.println("iter: " + i + " oid: " + oids[i]);
		    System.out.println("unexpected: " + e);
		    e.printStackTrace(System.out);
		    System.exit(1);
		}

		if (opsSeenInTrans++ == opsPerTrans) {
		    trans.commit();
		    opsSeenInTrans = 0;
		    trans = os.newTransaction(null);
		    trans.start();
		}
	    }
	}
	if (opsSeenInTrans > 0) {
	    trans.commit();
	}

	endTime = System.currentTimeMillis();

	long totalVisited = oids.length * laps;

	long elapsed = endTime - startTime;
	double ave = elapsed / (double) totalVisited;

	System.out.println("EE: elapsed " + elapsed +
		" totalOps " + totalVisited);

	System.out.println("RESULT ave " +
	    	    (doLock ? "LOCK: " : "PEEK: ") + ave + " ms" + 
		    " trans size " + opsPerTrans + " objSize " + params.objSize);

	System.out.println("draining...");
	os.close();
	System.out.println("drained.");
    }

    private static void lookupTest(long appID, long[] oids, TestParams params,
	    int opsPerTrans, int laps) {
	long startTime, endTime;
	Transaction trans = null;

	/* 
	 * The assumption here is that the oids were created by
	 * createNewBunch, starting with a blank slate, and therefore
	 * we know exactly what names each object has.  This is a big
	 * assumption...
	 */

	ObjectStore os = TestUtil.connect(appID, false,
		params.dataSpaceType, null);

	startTime = System.currentTimeMillis();

	int opsSeenInTrans = 0;
	trans = os.newTransaction(null);
	trans.start();

	for (int lap = 0; lap < laps; lap++) {
	    for (int i = 0; i < oids.length; i++) {

		String name = ObjectCreator.idString(appID, i);

		try {
		    long oid = trans.lookup(name); 
		    if (oid != oids[i]) {
			System.out.println("CHECK FAILED: " + oid + " != " +
				oids[i]);
		    }
		} catch (Exception e) {
		    System.out.println("iter: " + i + " oid: " + oids[i]);
		    System.out.println("unexpected: " + e);
		    e.printStackTrace(System.out);
		    System.exit(1);
		}

		if (opsSeenInTrans++ == opsPerTrans) {
		    trans.commit();
		    opsSeenInTrans = 0;
		    trans = os.newTransaction(null);
		    trans.start();
		}

	    }
	}

	if (opsSeenInTrans > 0) {
	    trans.commit();
	}
	endTime = System.currentTimeMillis();

	long totalVisited = oids.length * laps;
	long elapsed = endTime - startTime;
	double ave = elapsed / (double) totalVisited;

	System.out.println("EE: elapsed " + elapsed +
		" totalOps " + totalVisited);

	System.out.println("RESULT ave LOOKUP speed: " + ave + " ms" +
		" trans size " + opsPerTrans + " objSize " + params.objSize);
	os.close();
    }
}
