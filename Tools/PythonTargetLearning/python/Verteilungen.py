# -*- coding: utf-8 -*-
"""
Created on Fri Jul 13 12:55:42 2018

@author: Tim
"""
from matplotlib import pyplot as plt
import numpy as np
from mpl_toolkits.mplot3d import Axes3D #it is not unused!
import plotly as py
import plotly.graph_objs as go


def getTriangularNumber(max):
        return max - np.sqrt((1 - np.random.rand())*max**2)
    
def getExponentialNumber(max):
    #number =  np.log(1- np.random.rand())/-1
    number = -np.log(np.random.rand())
    '''
    if number > max:
        #return max
        return getExponentialNumber(max)
    else:
        return number
    '''
    return number


def getUniformNumber(max):
    return np.random.uniform(0.,max)
    

def summe_3(randomGenerator, iterations, title):
    left = []
    middle = []
    right = []
    for i in range(iterations):
    
        maximum = 1.

        left += [randomGenerator(maximum)]
        maximum -= left[-1]
    
        middle += [randomGenerator(maximum)]
        maximum -= middle[-1]
    
        right += [maximum]
        
    plotHeatmap(left, middle, title, iterations)
    
    return left, middle, right
''' 
    bin_width = 0.025
    plt.hist(left, bins=np.arange(min(left), max(left) + bin_width, bin_width))
    plt.show()
    
    plt.hist(middle, bins=np.arange(min(left), max(left) + bin_width, bin_width))
    plt.show()
    
    plt.hist(right, bins=np.arange(min(left), max(left) + bin_width, bin_width))
    plt.show()
'''
def summe_32(randomGenerator, iterations, title):
    
    probs = np.zeros((iterations,3))

    for i in range(iterations):
    
        arrayStart = np.random.randint(3)
        maximum = 1.
        
        for j in range(2):
            index = (arrayStart + j )%3
            probs[i][index] = randomGenerator(maximum)
            maximum -= probs[i][index]
            
        probs[i][(arrayStart + j + 1 )%3] = maximum
        
    plotHeatmap(probs[:,0],probs[:,1], title, iterations)


'''
    bin_width = 0.025
    plt.hist(probs[:,0], bins=np.arange(min(probs[:,0]), max(probs[:,0]) + bin_width, bin_width))
    plt.show()
    
    plt.hist(probs[:,1], bins=np.arange(min(probs[:,0]), max(probs[:,0]) + bin_width, bin_width))
    plt.show()
    
    plt.hist(probs[:,2], bins=np.arange(min(probs[:,0]), max(probs[:,0]) + bin_width, bin_width))
    plt.show()
'''


#    return left, middle, right
    
def normiert_3(randomGenerator, iterations, title):
    left = []
    middle = []
    right = []
    for i in range(iterations):
    
        left += [randomGenerator(1)]
        middle += [randomGenerator(1)]
        right += [randomGenerator(1)]
        
        sum = left[-1] + middle[-1] + right[-1]
        left[-1] /= sum
        middle[-1] /= sum 
        right[-1] /= sum    
    plotHeatmap(left, middle, title, iterations)
    
    '''
    bin_width = 0.025
    plt.hist(left, bins=np.arange(min(left), max(left) + bin_width, bin_width))
    plt.show()
    
    plt.hist(middle, bins=np.arange(min(left), max(left) + bin_width, bin_width))
    plt.show()
    
    plt.hist(right, bins=np.arange(min(left), max(left) + bin_width, bin_width))
    plt.show()
    '''
    return left, middle, right

    
def logData(heatmap):
    for row in range(len(heatmap)):
        for column in range(len(heatmap[row])):
            if heatmap[row][column] == 0:
                heatmap[row][column] = 1.0
    return np.log(heatmap)

def plotHeatmap(x,y, title, iterations):
    heatmap, xedges, yedges = np.histogram2d(x, y, bins=50, range = [[0,1],[0,1]])
    
    #heatmap = logData(heatmap)
    
    extent = [xedges[0], xedges[-1], yedges[0], yedges[-1]]
    plt.clf()
    if iterations/500 < 5:
        vmax = 5
    else:
        vmax = iterations/500
    plt.imshow(heatmap.T, extent=extent, origin='lower', vmin = 0, vmax = vmax)
    plt.colorbar()
    #plt.title(title)
    plt.xlabel('Anteil an Linksabbiegern')
    plt.ylabel('Anteil an Rechtsabbiegern')
    plt.savefig('C:/Studium/BA/Latex/Template_BA_Tests/pictures/tmp.pdf')
    plt.show()
    
def plot3dInteractive(x,y,z):
    data = [go.Scatter3d( x=x, y=y, z=z, mode='markers')]
    layout = go.Layout(
    margin=dict(l=0,r=0,b=0,t=0))
    fig = go.Figure(data=data, layout=layout)
    py.offline.plot(fig)


###################
# 3-way Vergleiche#
###################
iterations = 5000

#Gleichverteilt, summe
#summe_3(getUniformNumber, iterations, 'Gleichverteilt Summe')

#Dreieck, summe
#summe_3(getTriangularNumber, iterations, 'Dreieck Summe')

#Dreieck, summe, randomStart
#summe_32(getTriangularNumber, iterations, 'Dreieck Summe Random Start')

#Gleichverteilung, normiert
#normiert_3(getUniformNumber, iterations, 'Gleichverteilt Normiert')

#Dreieck, normiert
#normiert_3(getTriangularNumber, iterations, 'Dreiecksverteilung Normiert')

#Exponential normiert
normiert_3(getExponentialNumber, iterations, 'Exponentialverteilung Normiert') 

'''
left = []
middle = []
right = []
for i in range(iterations):

    maximum = 1.

    left += [getTriangularNumber(maximum)]
    maximum -= left[-1]

    middle += [getUniformNumber(maximum)]
    maximum -= middle[-1]

    right += [maximum]
    
plotHeatmap(left, middle, "Dreieck und Gleichverteilung", iterations)
'''


##############
#Verteilungen#  
##############

'''
#expo
erg = []
for _ in range(iterations):
    #erg += [1 - np.random.triangular(0,1,1)]
    erg += [getExponentialNumber(1)]

bin_width = 0.05
plt.hist(erg, bins=np.arange(min(erg), max(erg) + bin_width, bin_width))
plt.xlabel('Wertebereich')
plt.ylabel('Häufigkeit')
#plt.hist(erg)
plt.savefig('C:/Studium/BA/Latex/Template_BA_Tests/pictures/tmp.pdf')
plt.show()
'''

'''
#dreieck
erg = []
for _ in range (iterations):
    b = 1
    #erg += [b - np.sqrt((1 - np.random.rand())*b**2)]
    erg += [getTriangularNumber(1)]


bin_width = 0.05
plt.hist(erg, bins=np.arange(min(erg), max(erg) + bin_width, bin_width))
plt.xlabel('Wertebereich')
plt.ylabel('Häufigkeit')
#plt.hist(erg)
plt.savefig('C:/Studium/BA/Latex/Template_BA_Tests/pictures/tmp.pdf')
plt.show()
'''
    
    
###########
#Sonstiges#
###########

#euklid
#simulationen = np.array([10,25,50,100,200])
#simulationen2 = np.array([10,25,50,100,200, 250, 300])
#langerGang = np.array([0.205, 0.196,0.215,0.236,0.223, 0.218, 0.217])
#Bruecke = np.array([0.087, 0.084, 0.079, 0.069, 0.052])
#Asymmetrisch = np.array([0.147,0.154,0.122,0.106,0.086])


'''
simulationen = np.array([10,25,50,75,100,150,200,250,300])
langerGang = np.array([0.213,0.209,0.226,0.236,0.247,0.224,0.235,0.232,0.232])
Asymmetrisch= np.array([0.149,0.161,0.129,0.129,0.116,0.1,0.094,0.092,0.077])
Bruecke  = np.array([0.087,0.089,0.086,0.072,0.075,0.074,0.056,0.054,0.051])


plt.plot(simulationen, langerGang)
plt.plot(simulationen, Bruecke)
plt.plot(simulationen, Asymmetrisch)

plt.scatter(simulationen, langerGang, label = 'Kreuzung')
plt.scatter(simulationen, Bruecke, label = 'schiefer Gang')
plt.scatter(simulationen, Asymmetrisch, label = 'Asymmetrisch')

plt.xlabel('Anzahl an Simulationen')
plt.ylabel('Mittlerer euklidischer Fehler')
plt.legend()
plt.savefig('C:/Studium/BA/Latex/Template_BA_Tests/pictures/SampleSize.pdf')
plt.show()



simulationen = np.array([10,25,50,75,100,150,200,250,300])
langerGang = np.array([0.367,0.161,0.249,0.267,0.227,0.27,0.283,0.264,0.277])
Asymmetrisch= np.array([0.522,0.574,0.594,0.679,0.663,0.757,0.753,0.761,0.799])
Bruecke  = np.array([0.745,0.872,0.837,0.896,0.881,0.827,0.909,0.887,0.894])


plt.plot(simulationen, langerGang)
plt.plot(simulationen, Bruecke)
plt.plot(simulationen, Asymmetrisch)

plt.scatter(simulationen, langerGang, label = 'Kreuzung')
plt.scatter(simulationen, Bruecke, label = 'schiefer Gang')
plt.scatter(simulationen, Asymmetrisch, label = 'Asymmetrisch')

plt.xlabel('Anzahl an Simulationen')
plt.ylabel('Out of Bag Score')
plt.legend()
plt.savefig('C:/Studium/BA/Latex/Template_BA_Tests/pictures/oob.pdf')
plt.show()
'''




'''
#OOB
simulationen = np.array([10,25,50,100,200])
simulationen2 = np.array([10,25,50,100,200, 250, 300])
langerGang = np.array([])
Bruecke = np.array([])
Asymmetrisch = np.array([])

plt.plot(simulationen2, langerGang)
plt.plot(simulationen, Bruecke)
plt.plot(simulationen, Asymmetrisch)

plt.scatter(simulationen2, langerGang, label = 'Langer Gang')
plt.scatter(simulationen, Bruecke, label = 'Brücke')
plt.scatter(simulationen, Asymmetrisch, label = 'Asymmetrisch')

plt.xlabel('Anzahl an Simulationen')
plt.ylabel('Mittlerer Euklidischer Fehler')
plt.legend()
plt.show()
'''





'''
#Exponential normiert 2-way
left = []
middle = []
for i in range(iterations):
    
    left += [getExponentialNumber(1)]
    middle += [getExponentialNumber(1)]
    
    sum = left[-1] + middle[-1]
    left[-1] /= sum
    middle[-1] /= sum 
    
plotHeatmap(left, middle, 'Exponential normiert')
'''

#Exponential normiert 3-way
#left, middle, right = normiert_3(getExponentialNumber, iterations, 'Exponentialverteilung Normiert') 
#plot3dInteractive(left,middle,right)



'''
#Dreieck, normiert 4-way
left = []
middle = []
right = []
down = []
for i in range(iterations):
    
    left += [getRandomNumber(1)]
    middle += [getRandomNumber(1)]
    right += [getRandomNumber(1)]
    down += [getRandomNumber(1)]
    
    sum = left[-1] + middle[-1] + right[-1] + down[-1]
    left[-1] /= sum
    middle[-1] /= sum 
    right[-1] /= sum 
    down[-1] /= sum
#plotHeatmap(left, middle, 'Dreieck normiert')
plot3dInteractive(left,middle,right)
'''


'''
#Exponential normiert 4-way
left = []
middle = []
right = []
down = []
for i in range(iterations):
    
    left += [getExponentialNumber(1)]
    middle += [getExponentialNumber(1)]
    right += [getExponentialNumber(1)]
    down += [getExponentialNumber(1)]
    
    sum = left[-1] + middle[-1] + right[-1] + down [-1]
    left[-1] /= sum
    middle[-1] /= sum 
    right[-1] /= sum  
    down[-1] /= sum  
    
#fig = plt.figure()
#ax = fig.add_subplot(111, projection='3d')
#ax.scatter(left, middle, right)
#plt.show()
plot3dInteractive(left,middle,right)
'''















