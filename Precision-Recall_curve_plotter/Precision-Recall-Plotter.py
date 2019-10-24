import numpy as np
import matplotlib.pyplot as plt

import pandas as pd


def main():

    fig, ax = plt.subplots(1, 1)
    ax.legend(["Jaccard", "TF-IDF", "BM25"])
    ax.set_xlabel("Recall")
    ax.set_ylabel("Precision")

    urls = [("../evaluationTo50-JaccardCoeff.csv", "purple"), ("../evaluationTo50-TfIdf.csv", "blue"), ("../evaluationTo50-BM25.csv", "green")]

    for pair in urls:
        plotPrecisionRecallCurve(ax, pair[0], pair[1])

    plt.savefig('fig.png')
    fig.show()


def plotPrecisionRecallCurve(ax, urls, color):
    df = pd.read_csv(urls)
    df.head()
    tfIdfArray = df.values
    recall = tfIdfArray[:, 2]
    precision = tfIdfArray[:, 1]

    # take a running maximum over the reversed vector of precision values, reverse the
    # result to match the order of the recall vector
    decreasing_max_precision = np.maximum.accumulate(precision[::-1])[::-1]

    interpolatedPrecision = interpolate(precision, recall)

    # plotting...
    # ax.hold(True)
    ax.plot(recall, interpolatedPrecision, '--b')
    ax.step(recall, decreasing_max_precision, '-r')
    ax.plot(recall, precision, 'k--', color=color)


def interpolate(precision, recall):
    interpolated = precision.copy()
    i = recall.shape[0] - 2
    # interpolation...
    while i >= 0:
        if interpolated[i + 1] > interpolated[i]:
            interpolated[i] = interpolated[i + 1]
        i = i - 1
    return interpolated


if __name__ == '__main__':
    main()
