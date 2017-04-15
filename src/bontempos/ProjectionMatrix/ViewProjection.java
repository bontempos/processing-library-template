package bontempos.ProjectionMatrix;

import Jama.*;
import processing.core.PMatrix3D;
import processing.core.PVector;



/**
 * This generates a View * Projection matrix
 * to be multiplied by a model matrix (identity) and use
 *
 * @example Hello 
 */

public class ViewProjection {

	public static boolean printMatrix = false;
	public static boolean flipped = false;
	public int calibrationPointsNum = 6; //default
	public static ViewProjection instance;



	public ViewProjection() {}




	public static PMatrix3D get(PVector[] p2d, PVector[]p3d) {
		if (instance == null) {
			instance = new ViewProjection();
			return instance.getViewProjectionMatrix(p2d, p3d);
		}
		return instance.getViewProjectionMatrix(p2d, p3d);
	}



	public static PVector solve(PVector input, PMatrix3D projMatrix) {
		if (instance == null) {
			instance = new ViewProjection();;
			return instance.getProjectedCoord(input, projMatrix);
		}
		return instance.getProjectedCoord(input, projMatrix);
	}

	
	public static void printMatrix(boolean b){
		if (instance == null){
			instance = new ViewProjection();
			instance.printMatrix = b;
		}else{
			instance.printMatrix = b;
		}
	}
	

	public static void flipped(boolean b){
		if (instance == null){
			instance = new ViewProjection();
			instance.flipped = b;
		}else{
			instance.flipped = b;
		}
	}
	
	


	/**
	 * builds the left side of the matrix to be computed for 2D and 3D correspondence
	 * @param p2d the points in a plane correspondent to 3D points projection
	 * @param p3d the spatial 3D points
	 */
	double [][] makeMatrixA3D ( PVector[] p2d, PVector[]p3d ) {

		int ptsNum = p2d.length;

		double [] tmpAc = new double[ ptsNum * 11 * 2 ];
		int j = 0;
		for (int i = 0; i < ptsNum; i++) {
			float X, Y, Z, u, v;
			X = p3d[i].x;
			Y = p3d[i].y;
			Z = p3d[i].z;
			u = p2d[i].x;
			v = p2d[i].y;
			//inserting vars for Projector u
			double[] Pu = {X, Y, Z, 1, 0, 0, 0, 0, -X*u, -Y*u, -Z*u};
			for (int k = 0; k < Pu.length; k++) {
				tmpAc[j] = Pu[k];
				j++;
			}
			//inserting vars for Projector v
			double[] Pv = { 0, 0, 0, 0, X, Y, Z, 1, -X*v, -Y*v, -Z*v};
			for (int k = 0; k < Pv.length; k++) { 
				tmpAc[j] = Pv[k];
				j++;
			}
		}
		//formating result maxCpoints*2 rows 11 cols
		int row = 0, col = 0;
		double[][] m = new double[ptsNum*2][11];
		for (int i = 0; i < tmpAc.length; i++) {

			m[row][col] = tmpAc[i];

			col++;
			if (col >= 11) {
				row++;
				col = 0;
			}
		} 
		return m;
	}






	/**
	 * builds the right side of the matrix to be computed
	 */
	double[][] makeMatrixR(PVector [] vec2d) { //just one row, but using [][] is for convenience.
		int j = 0;
		int ptsNum = vec2d.length;
		double [][] m = new double[ptsNum*2][1]; //this is  u and v from projector (only col 0 is used)

		for (int i = 0; i < ptsNum; i++) {
			m[j][0] =  vec2d[i].x;
			j++;
			m[j][0] =  vec2d[i].y;
			j++;
		}
		return m;
	}



	/**
	 * below is this equation ProjMatrix = ( A.transpose * A ).invert() * ( A.transpose * R )
	 * @param ac Matrix left side
	 * @param rc Matrix right side
	 * @return PMatrix3D projection matrix
	 */
	PMatrix3D makeProjectionMatrix(double[][] ac, double[][]rc) {

		Matrix A = new Matrix(ac);
		Matrix R = new Matrix(rc);
		
		if(printMatrix){
			A.print(7, 1);
			R.print(7, 1);
		}

		try{
			Matrix tmpA = A.transpose().times(A);
			Matrix tmpR = A.transpose().times(R);
			Matrix result = tmpA.inverse().times(tmpR);

			PMatrix3D projMat = new PMatrix3D(
					(float)result.get(0, 0), (float)result.get(1, 0), (float)result.get(2, 0), (float)result.get(3, 0), 
					(float)result.get(4, 0), (float)result.get(5, 0), (float)result.get(6, 0), (float)result.get(7, 0), 
					0f, 0f, (flipped)?1f:-1f, 1f,
					(float)result.get(8, 0), (float)result.get(9, 0), (float)result.get(10, 0), 1f );
			
			
			if(printMatrix){
				result.print(7, 5);
			}
			
			return projMat;
		}catch(RuntimeException e){
			System.out.println("Failed creating ViewProjection Matrix");
			return null;
		}
	}

	/**
	 * uses the given array of 2D projected points and makes correspondences to
	 * to given 3D points in the given order.
	 * @param p2d is an array with 2D points 
	 * @param p3d is an array with 3D points
	 * @return PMatrix3D projection matrix
	 */
	 PMatrix3D getViewProjectionMatrix( PVector[] p2d, PVector[]p3d ) {

		if( p3d.length > calibrationPointsNum){
			calibrationPointsNum = p3d.length;
		}

		if( p2d.length == p3d.length){
			return 	makeProjectionMatrix( makeMatrixA3D(p2d, p3d), makeMatrixR(p2d) );
		}

		else{
			return null;
		}
	}




	/**
	 * converts 3D coordinate to its correspondent projection in 2D coord
	 * @param input 3D point in space
	 * @param projMatrix projection matrix to be used
	 * @return output 2D point in projection coord.
	 */
	PVector getProjectedCoord(PVector input, PMatrix3D projMatrix) {
		float [] in = { input.x, input.y, input.z, 1f };
		float [] out = new float[4];
		projMatrix.mult(in, out);
		return new PVector( out[0]/out[3], out[1]/out[3] ); 
	}












}
