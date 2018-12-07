import tensorflow as tf;

tf.enable_eager_execution();
print(tf.reduce_sum(tf.random_normal([1000, 1000])));