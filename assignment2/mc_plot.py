import numpy as np
import matplotlib.pyplot as plt


def readFile(fileName):
    x_iteration = []
    y_pagerank = []
    try:
        mcfile = open(fileName, 'r')
        while True:
            line = mcfile.readline()
            if not line:
                break
            strs = line.split(';')
            x_iteration.append(float(strs[0]))
            y_pagerank.append(float(strs[1]))
    finally:
        mcfile.close()

    return x_iteration, y_pagerank


def readPageRank(fileName):
    x_iteration = []
    y_pagerank = []
    try:
        mcfile = open(fileName, 'r')
        for i in range(10):
            line = mcfile.readline()
            if not line:
                break
            strs = line.split(';')
            x_iteration.append(float(strs[0]))
            y_pagerank.append(float(strs[1]))
    finally:
        mcfile.close()

    return x_iteration, y_pagerank


def readFileEx(fileName):
    x_iteration = []
    y_pagerank = []
    try:
        mcfile = open(fileName, 'r')
        while True:
            line = mcfile.readline()
            if not line:
                break
            strs = line.split(' ')
            x_iteration.append(strs[0])
            y_pagerank.append(float(strs[1]))
    finally:
        mcfile.close()

    return x_iteration, y_pagerank


def plot_mc(fileName, label):
    # x_plot, y_plot = readPageRank(fileName)
    x_plot, y_plot = readFile(fileName)
    plt.xlabel("N")
    plt.ylabel("Squared differences")
    plt.plot(x_plot, y_plot, label=label)
    plt.grid()
    # plt.show()


def getDistance(fileName):
    x, y = readFileEx(fileName)
    euclidean = 0
    manhattan = 0
    for i in range(len(y)):
        euclidean += y[i] ** 2
        manhattan += y[i]
    euclidean = np.sqrt(euclidean)
    print("Euclidean: ", euclidean, " Manhattan: ", manhattan)
    print(len(y))


def PR(fileName):
    x_num = []
    y_precision = []
    y_recall = []
    all_relevant = 100
    try:
        mcfile = open(fileName, 'r')
        num_of_file = 1
        num_of_relevant = 0
        while True:
            line = mcfile.readline()
            if not line:
                break
            strs = line.split(' ')
            x_num.append(num_of_file)
            if float(strs[2]) > 0:
                num_of_relevant += 1
            y_precision.append(num_of_relevant / num_of_file)
            y_recall.append(num_of_relevant / all_relevant)
            num_of_file += 1
    finally:
        mcfile.close()

    plt.xlabel("num of files")
    plt.ylabel("precision")
    plt.plot(x_num, y_precision, 'b-')
    plt.grid()
    plt.show()

    plt.xlabel("num of files")
    plt.ylabel("recall")
    plt.plot(x_num, y_recall, 'b-')
    plt.grid()
    plt.show()

    plt.xlabel("precision")
    plt.ylabel("recall")
    plt.plot(y_precision, y_recall, 'b-')
    plt.grid()
    plt.show()

def main():
    # getDistance("M-Token.txt")
    # getDistance("N-Token.txt")
    # PR("Zehua.txt")
    plot_mc("MC1-measure", "MC1")
    plot_mc("MC2-measure", "MC2")
    plot_mc("MC4-measure", "MC4")
    plot_mc("MC5-measure", "MC5")
    plt.legend()
    plt.grid()
    plt.show()


main()

