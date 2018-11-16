import numpy as np


def calc_and_print_errors_rf(y_test_density, y_predicted_normiert, log_file, score_training, score_test, score_oob):
    log_file.write(
        "Accuracy on training set (score): [%f %f %f]\n" % (score_training[0], score_training[1], score_training[2]))
    log_file.write("Accuracy on test set     (score): [%f %f %f] \n" % (score_test[0], score_test[1], score_test[2]))
    log_file.write("Out of bag error         (score): [%f %f %f] \n\n" % (score_oob[0], score_oob[1], score_oob[2]))

    calc_and_print_errors(y_test_density, y_predicted_normiert, log_file)

def calc_and_print_errors(y_test_density, y_predicted_normiert, log_file):


    ### Calc error of prediction

    # print importance of features
    # print("Importance of features")
    # print(rf_density_regressor.feature_importances_)

    # Euclidean error over all forests and samples
    euklid_error = np.sqrt(np.sum(np.square(y_predicted_normiert - y_test_density), 1))
    euklid_error_mean = np.mean(euklid_error)
    euklid_error_std = np.std(euklid_error)
    euklid_error_mean_percent = euklid_error_mean / np.sqrt(2) * 100

    # Daniel: Mean absolute error
    mean_abs_error = np.mean(np.abs(y_predicted_normiert - y_test_density), axis=0)
    mean_abs_error_percent = mean_abs_error * 100
    mean_error = np.mean(y_predicted_normiert - y_test_density, axis=0)

    # RMSE (per Tree)
    rmse_per_tree = np.sqrt(np.mean(np.square(y_predicted_normiert - y_test_density), axis=0))


    ### Print to console
    print("RMSE (per direction): [%f %f %f]" % (rmse_per_tree[0], rmse_per_tree[1], rmse_per_tree[2]))
    print("RMSE (per direction): [%.2f%% %.2f%% %.2f%%]\n" % (
    rmse_per_tree[0] * 100, rmse_per_tree[1] * 100, rmse_per_tree[2] * 100))

    print("Mean Euclidean Error: %f" % euklid_error_mean)
    print("Mean Euclidean Error: %.2f%%" % euklid_error_mean_percent)
    print("Std  Euclidean Error: %f " % euklid_error_std)
    print("Std  Euclidean Error: %.2f%% \n" % (euklid_error_std / np.sqrt(2) * 100))

    print(
        "Mean Absolute Error (per direction): [%f %f %f] " % (mean_abs_error[0], mean_abs_error[1], mean_abs_error[2]))
    print("Mean Absolute Error (per direction): [%.2f%% %.2f%% %.2f%%]" % (
    mean_abs_error_percent[0], mean_abs_error_percent[1], mean_abs_error_percent[2]))
    print("Mean Error          (per direction): [%f %f %f]" % (mean_error[0], mean_error[1], mean_error[2]))


    ###### Write results to file

    log_file.write(" * Errors on test data set: \n\n")

    log_file.write("RMSE (per direction): [%f %f %f] \n" % (rmse_per_tree[0], rmse_per_tree[1], rmse_per_tree[2]))
    log_file.write("RMSE (per direction): [%.2f%% %.2f%% %.2f%%] \n\n" % (
    rmse_per_tree[0] * 100, rmse_per_tree[1] * 100, rmse_per_tree[2] * 100))

    log_file.write("Mean Euclidean Error: %f \n" % euklid_error_mean)
    log_file.write("Mean Euclidean Error: %.2f%% \n" % euklid_error_mean_percent)
    log_file.write("Std  Euclidean Error: %f \n" % euklid_error_std)
    log_file.write("Std  Euclidean Error: %.2f%% \n\n" % (euklid_error_std / np.sqrt(2) * 100))

    log_file.write(
        "Mean Absolute Error (per forest): [%f %f %f] \n" % (mean_abs_error[0], mean_abs_error[1], mean_abs_error[2]))
    log_file.write("Mean Absolute Error (per direction): [%.2f%% %.2f%% %.2f%%] \n" % (
    mean_abs_error_percent[0], mean_abs_error_percent[1], mean_abs_error_percent[2]))
    log_file.write("Mean Error          (per direction): [%f %f %f] \n" % (mean_error[0], mean_error[1], mean_error[2]))

    log_file.flush()



