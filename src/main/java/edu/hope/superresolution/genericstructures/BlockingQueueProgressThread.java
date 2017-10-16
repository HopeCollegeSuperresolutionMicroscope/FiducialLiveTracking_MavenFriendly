/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.genericstructures;

import edu.valelab.gaussianfit.data.SpotData;
import java.util.concurrent.BlockingQueue;

/**
 * 
 * Cannibalized Class From GaussianFit Code.
 * <p>
 * Modified to be Generic, but not modified to prevent loss of info if multiple threads called
 * That is a TODO.
 * 
 * credit: Nico
 */

public class BlockingQueueProgressThread<QueueData>  implements Runnable {

   Thread t_;
   BlockingQueue<QueueData> sourceList_;



   public BlockingQueueProgressThread(BlockingQueue<QueueData> sourceList) {
      sourceList_ = sourceList;
   }

   public void init() {
      t_ = new Thread(this);
      t_.start();
   }

   public void join() throws InterruptedException {
      if (t_ != null)
         t_.join();
   }

   
   @Override
   public void run() {
      int maxNr = sourceList_.size();
      int size = maxNr;
      while (sourceList_ != null && size > 0) {
         ij.IJ.wait(2000);
         size = sourceList_.size();
         ij.IJ.showStatus("Fitting remaining Gaussians...");
         ij.IJ.showProgress(maxNr - size, maxNr);
      }
      ij.IJ.showStatus("");
   }
}
