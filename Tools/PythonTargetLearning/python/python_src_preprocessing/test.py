import numpy as np
import matplotlib.pyplot as plt
x = np.linspace(0, 7.0, num=10)
y = np.cos(2*x)

plt.figure()
plt.plot(x,y,'b-.')
plt.xlabel("Time [s]")
plt.ylabel("Signal")
plt.show()